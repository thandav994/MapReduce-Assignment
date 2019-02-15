package assignment1b.main;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class Part2 {
	public static class MovieRatingMapper extends Mapper<LongWritable, Text, LongWritable, DoubleWritable> {

		private DoubleWritable rating = new DoubleWritable();
		private LongWritable movieId = new LongWritable();

		public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
			if (key.get() == 0 && value.toString().contains("rating")) // Ignores the first line with headers
				return;
			String[] line = value.toString().split(",");
			movieId.set(Long.parseLong(line[1]));
			rating.set(Double.parseDouble(line[2]));
			context.write(movieId, rating);
		}
	}

	public static class RatingAverageReducer extends Reducer<LongWritable, DoubleWritable, LongWritable, DoubleWritable> {
		private Map<LongWritable, DoubleWritable> averageMap = new HashMap<LongWritable, DoubleWritable>();

		public void reduce(LongWritable key, Iterable<DoubleWritable> values, Context context)
				throws IOException, InterruptedException {
			double sum = 0;
			int count = 0;
			for (DoubleWritable val : values) {
				sum += val.get();
				count += 1;
			}
			double average = sum / count;
			averageMap.put(new LongWritable(key.get()), new DoubleWritable(average));
		}

		@Override
		protected void cleanup(Context context) throws IOException, InterruptedException {

			Map<LongWritable, DoubleWritable> sortedMap = MiscUtils.sortByValues(averageMap);

			int counter = 0;
			for (LongWritable key : sortedMap.keySet()) {
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
		Job job = Job.getInstance(conf, "Part 2");
		job.setJarByClass(Part2.class);
		job.setMapperClass(MovieRatingMapper.class);
		job.setReducerClass(RatingAverageReducer.class);
		job.setOutputKeyClass(LongWritable.class);
		job.setOutputValueClass(DoubleWritable.class);
		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
}
