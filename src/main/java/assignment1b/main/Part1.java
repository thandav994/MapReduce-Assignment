package assignment1b.main;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class Part1 {
	public static HashSet<String> stopWords = null;

	public static class TokenizerMapper extends Mapper<Object, Text, Text, IntWritable> {

		private final static IntWritable one = new IntWritable(1);
		private Text word = new Text();

		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
			StringTokenizer itr = new StringTokenizer(value.toString());
			String token;
			while (itr.hasMoreTokens()) {
				token = itr.nextToken().toLowerCase();
				// Removing all symbols
				token = token.replaceAll("[^A-Za-z0-9]", "");
				if (!isAStopWord(token) && token.length() >= 5) {
					word.set(token);
					context.write(word, one);
				}
			}
		}
	}

	public static void populateStopWords() {
		Scanner sc = null;
		try {
			sc = new Scanner(new URL("https://www.textfixer.com/tutorials/common-english-words-with-contractions.txt")
					.openStream(), "UTF-8");
			String stopWordsString = sc.useDelimiter("\\A").next();
			// Removing symbols in stop words
			stopWordsString = stopWordsString.replaceAll("[^A-Za-z0-9,]", "");
			stopWords = new HashSet<String>(Arrays.asList(stopWordsString.split(",")));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			sc.close();
		}
	}

	public static boolean isAStopWord(String word) {
		return stopWords.contains(word);
	}

	public static class IntSumReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
		private IntWritable result = new IntWritable();
		private Map<Text, IntWritable> countMap = new HashMap<Text, IntWritable>();
		
		public void reduce(Text key, Iterable<IntWritable> values, Context context)
				throws IOException, InterruptedException {
			int sum = 0;
			
			for (IntWritable val : values) {
				sum += val.get();
			}
			result.set(sum);
			countMap.put(new Text(key), result);
		}
		
		@Override
        protected void cleanup(Context context) throws IOException, InterruptedException {

            Map<Text, IntWritable> sortedMap = MiscUtils.sortByValues(countMap);

            int counter = 0;
            for (Text key : sortedMap.keySet()) {
                if (counter++ == 20) {
                    break;
                }
                context.write(key, sortedMap.get(key));
            }
        }
	}
	

	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		conf.set("mapred.job.tracker", "hdfs://cshadoop1:61120");
		conf.set("yarn.resourcemanager.address", "cshadoop1.utdallas.edu:8032");
		conf.set("mapreduce.framework.name", "yarn");
		Job job = Job.getInstance(conf, "Part 1");
		populateStopWords();				// Populates stop words
		job.setJarByClass(Part1.class);
		job.setMapperClass(TokenizerMapper.class);
		job.setCombinerClass(IntSumReducer.class);
		job.setReducerClass(IntSumReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);
		FileInputFormat.setInputDirRecursive(job, true);
		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));	
		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
}
