package org.datasyslab.geospark.formatMapper.shapefileParser.boundary;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.CombineFileSplit;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class BoundaryRecordReader extends RecordReader<Long, BoundBox>{

    /** paths of files to be read */
    Path[] paths = null;

    /** fixed key value for reduce all results together */
    long KEY_VALUE = 0;

    /** input stream */
    FSDataInputStream inputStream = null;

    /** task context */
    Configuration configuration = null;

    /** index of current file to be read */
    int id = 0;


    @Override
    public void initialize(InputSplit inputSplit, TaskAttemptContext taskAttemptContext) throws IOException, InterruptedException {
        CombineFileSplit split = (CombineFileSplit)inputSplit;
        paths = split.getPaths();
        configuration = taskAttemptContext.getConfiguration();
        id = 0;
    }

    @Override
    public boolean nextKeyValue() throws IOException, InterruptedException {
        id++;
        return id < paths.length;
    }

    @Override
    public Long getCurrentKey() throws IOException, InterruptedException {
        return KEY_VALUE;
    }

    @Override
    public BoundBox getCurrentValue() throws IOException, InterruptedException {
        // open id file
        FileSystem fs = paths[id].getFileSystem(configuration);
        inputStream = fs.open(paths[id]);
        // skip first 36 bytes of head, read 64 bytes we need
        inputStream.skipBytes(36);
        byte[] bytes = new byte[64];
        inputStream.readFully(bytes);
        inputStream.close();
        // use byte buffer to abstract 8 parameters of bound box.
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        double[] bounds = new double[8];
        buffer.asDoubleBuffer().get(bounds);
        bounds = new double[8];
        return new BoundBox(bounds);
    }

    @Override
    public float getProgress() throws IOException, InterruptedException {
        return (float)id / (float)paths.length;
    }

    @Override
    public void close() throws IOException {
        // input stream already closed every time getCurrentKey()
    }
}
