package com.samin.carerobot.Service;

import android.os.Process;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

public class CoAISerialInputOutputManager implements Runnable {
    private static final String TAG = SerialInputOutputManager.class.getSimpleName();
    public static boolean DEBUG = false;
    private static final int BUFSIZ = 4096;

    /**
     * default read timeout is infinite, to avoid data loss with bulkTransfer API
     */
    private int mReadTimeout = 0;
    private int mWriteTimeout = 0;

    private final Object mReadBufferLock = new Object();
    private final Object mWriteBufferLock = new Object();

    private ByteBuffer mReadBuffer; // default size = getReadEndpoint().getMaxPacketSize()
//    private ByteBuffer mWriteBuffer = ByteBuffer.allocate(BUFSIZ);
    private ConcurrentLinkedQueue<ByteBuffer> mWriteBuffers = new ConcurrentLinkedQueue<>();

    public enum State {
        STOPPED,
        RUNNING,
        STOPPING
    }

    private int mThreadPriority = Process.THREAD_PRIORITY_URGENT_AUDIO;
    private SerialInputOutputManager.State mState = SerialInputOutputManager.State.STOPPED; // Synchronized by 'this'
    private SerialInputOutputManager.Listener mListener; // Synchronized by 'this'
    private final UsbSerialPort mSerialPort;

    public interface Listener {
        /**
         * Called when new incoming data is available.
         */
        void onNewData(byte[] data);

        /**
         * Called when {@link SerialInputOutputManager#run()} aborts due to an error.
         */
        void onRunError(Exception e);
    }

    public CoAISerialInputOutputManager(UsbSerialPort serialPort) {
        mSerialPort = serialPort;
        mReadBuffer = ByteBuffer.allocate(serialPort.getReadEndpoint().getMaxPacketSize());
    }

    public CoAISerialInputOutputManager(UsbSerialPort serialPort, SerialInputOutputManager.Listener listener) {
        mSerialPort = serialPort;
        mListener = listener;
        mReadBuffer = ByteBuffer.allocate(serialPort.getReadEndpoint().getMaxPacketSize());
    }

    public synchronized void setListener(SerialInputOutputManager.Listener listener) {
        mListener = listener;
    }

    public synchronized SerialInputOutputManager.Listener getListener() {
        return mListener;
    }

    /**
     * setThreadPriority. By default a higher priority than UI thread is used to prevent data loss
     *
     * @param threadPriority  see {@link Process#setThreadPriority(int)}
     * */
    public void setThreadPriority(int threadPriority) {
        if (mState != SerialInputOutputManager.State.STOPPED)
            throw new IllegalStateException("threadPriority only configurable before SerialInputOutputManager is started");
        mThreadPriority = threadPriority;
    }

    /**
     * read/write timeout
     */
    public void setReadTimeout(int timeout) {
        // when set if already running, read already blocks and the new value will not become effective now
        if(mReadTimeout == 0 && timeout != 0 && mState != SerialInputOutputManager.State.STOPPED)
            throw new IllegalStateException("readTimeout only configurable before SerialInputOutputManager is started");
        mReadTimeout = timeout;
    }

    public int getReadTimeout() {
        return mReadTimeout;
    }

    public void setWriteTimeout(int timeout) {
        mWriteTimeout = timeout;
    }

    public int getWriteTimeout() {
        return mWriteTimeout;
    }

    /**
     * read/write buffer size
     */
    public void setReadBufferSize(int bufferSize) {
        if (getReadBufferSize() == bufferSize)
            return;
        synchronized (mReadBufferLock) {
            mReadBuffer = ByteBuffer.allocate(bufferSize);
        }
    }

    public int getReadBufferSize() {
        return mReadBuffer.capacity();
    }

//    public void setWriteBufferSize(int bufferSize) {
//        if(getWriteBufferSize() == bufferSize)
//            return;
//
//        synchronized (mWriteBufferLock) {
//            ByteBuffer newWriteBuffer = ByteBuffer.allocate(bufferSize);
//            if(mWriteBuffer.position() > 0)
//                newWriteBuffer.put(mWriteBuffer.array(), 0, mWriteBuffer.position());
//            mWriteBuffer = newWriteBuffer;
//        }
//    }

//    public int getWriteBufferSize() {
//        return mWriteBuffer.capacity();
//    }

    /**
     * when using writeAsync, it is recommended to use readTimeout != 0,
     * else the write will be delayed until read data is available
     */
    public void writeAsync(byte[] data) {
        synchronized (mWriteBufferLock) {
            mWriteBuffers.offer(ByteBuffer.wrap(data));
        }
    }

    /**
     * start SerialInputOutputManager in separate thread
     */
    public void start() {
        if(mState != SerialInputOutputManager.State.STOPPED)
            throw new IllegalStateException("already started");
        new Thread(this, this.getClass().getSimpleName()).start();
    }

    /**
     * stop SerialInputOutputManager thread
     *
     * when using readTimeout == 0 (default), additionally use usbSerialPort.close() to
     * interrupt blocking read
     */
    public synchronized void stop() {
        if (getState() == SerialInputOutputManager.State.RUNNING) {
            Log.i(TAG, "Stop requested");
            mState = SerialInputOutputManager.State.STOPPING;
        }
    }

    public synchronized SerialInputOutputManager.State getState() {
        return mState;
    }

    /**
     * Continuously services the read and write buffers until {@link #stop()} is
     * called, or until a driver exception is raised.
     */
    @Override
    public void run() {
        synchronized (this) {
            if (getState() != SerialInputOutputManager.State.STOPPED) {
                throw new IllegalStateException("Already running");
            }
            mState = SerialInputOutputManager.State.RUNNING;
        }
        Log.i(TAG, "Running ...");
        try {
            if(mThreadPriority != Process.THREAD_PRIORITY_DEFAULT)
                Process.setThreadPriority(mThreadPriority);
            while (true) {
                if (getState() != SerialInputOutputManager.State.RUNNING) {
                    Log.i(TAG, "Stopping mState=" + getState());
                    break;
                }
                step();
                Thread.sleep(5);
            }
        } catch (Exception e) {
            Log.w(TAG, "Run ending due to exception: " + e.getMessage(), e);
            final SerialInputOutputManager.Listener listener = getListener();
            if (listener != null) {
                listener.onRunError(e);
            }
        } finally {
            synchronized (this) {
                mState = SerialInputOutputManager.State.STOPPED;
                Log.i(TAG, "Stopped");
            }
        }
    }

    private void step() throws IOException {
        // Handle incoming data.
        byte[] buffer;
        synchronized (mReadBufferLock) {
            buffer = mReadBuffer.array();
        }
        int len = mSerialPort.read(buffer, mReadTimeout);
        if (len > 0) {
            if (DEBUG) Log.d(TAG, "Read data len=" + len);
            final SerialInputOutputManager.Listener listener = getListener();
            if (listener != null) {
                final byte[] data = new byte[len];
                System.arraycopy(buffer, 0, data, 0, len);
                listener.onNewData(data);
            }
        }

        // Handle outgoing data.
        buffer = null;
        synchronized (mWriteBufferLock) {
            while (true) {
                try {
                    ByteBuffer tmp = mWriteBuffers.poll();

                    if (tmp == null)
                        break;

                    mSerialPort.write(tmp.array(), mWriteTimeout);
                    tmp.clear();
                    Thread.sleep(0);

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public void writeBufferClear() {
        synchronized (mWriteBufferLock) {
            mWriteBuffers.clear();
        }
    }

}
