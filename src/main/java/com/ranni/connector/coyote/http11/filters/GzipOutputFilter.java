/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.ranni.connector.coyote.http11.filters;


import com.ranni.connector.coyote.Response;
import com.ranni.connector.coyote.http11.HttpOutputBuffer;
import com.ranni.connector.coyote.http11.OutputFilter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.zip.GZIPOutputStream;

/**
 * Gzip output filter.
 *
 * @author Remy Maucherat
 */
public class GzipOutputFilter implements OutputFilter {

    // ----------------------------------------------------- Instance Variables

    /**
     * Next buffer in the pipeline.
     */
    protected HttpOutputBuffer buffer;


    /**
     * Compression output stream.
     */
    protected GZIPOutputStream compressionStream = null;


    /**
     * Fake internal output stream.
     */
    protected final OutputStream fakeOutputStream = new FakeOutputStream();


    // --------------------------------------------------- OutputBuffer Methods

    @Override
    public int doWrite(ByteBuffer chunk) throws IOException {
        if (compressionStream == null) {
            compressionStream = new GZIPOutputStream(fakeOutputStream, true);
        }
        int len = chunk.remaining();
        if (chunk.hasArray()) {
            compressionStream.write(chunk.array(), chunk.arrayOffset() + chunk.position(), len);
            chunk.position(chunk.position() + len);
        } else {
            byte[] bytes = new byte[len];
            chunk.put(bytes);
            compressionStream.write(bytes, 0, len);
        }
        return len;
    }


    @Override
    public long getBytesWritten() {
        return buffer.getBytesWritten();
    }


    // --------------------------------------------------- OutputFilter Methods

    /**
     * Added to allow flushing to happen for the gzip'ed outputstream
     */
    @Override
    public void flush() throws IOException {
        if (compressionStream != null) {
            try {
                compressionStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        buffer.flush();
    }


    @Override
    public void setResponse(Response response) {
        // NOOP: No need for parameters from response in this filter
    }


    @Override
    public void setBuffer(HttpOutputBuffer buffer) {
        this.buffer = buffer;
    }


    @Override
    public void end() throws IOException {
        if (compressionStream == null) {
            compressionStream = new GZIPOutputStream(fakeOutputStream, true);
        }
        compressionStream.finish();
        compressionStream.close();
        buffer.end();
    }


    /**
     * Make the filter ready to process the next request.
     */
    @Override
    public void recycle() {
        // Set compression stream to null
        compressionStream = null;
    }


    // ------------------------------------------- FakeOutputStream Inner Class


    protected class FakeOutputStream
        extends OutputStream {
        protected final ByteBuffer outputChunk = ByteBuffer.allocate(1);
        @Override
        public void write(int b)
            throws IOException {
            // Shouldn't get used for good performance, but is needed for
            // compatibility with Sun JDK 1.4.0
            outputChunk.put(0, (byte) (b & 0xff));
            buffer.doWrite(outputChunk);
        }
        @Override
        public void write(byte[] b, int off, int len)
            throws IOException {
            buffer.doWrite(ByteBuffer.wrap(b, off, len));
        }
        @Override
        public void flush() throws IOException {/*NOOP*/}
        @Override
        public void close() throws IOException {/*NOOP*/}
    }


}
