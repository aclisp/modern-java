package fqueue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An `unmap` that is compatible with all versions of JDKs.
 * https://copyprogramming.com/howto/how-to-unmap-a-file-from-memory-mapped-using-filechannel-in-java
 */
public class MappedByteBufferUtil {
    private final static Logger logger = LoggerFactory.getLogger(MappedByteBufferUtil.class);

    public static void clean(final MappedByteBuffer buffer) {

        boolean isOldJDK = System.getProperty("java.specification.version", "99").startsWith("1.");
        try {
            if (isOldJDK) {
                Method cleaner = buffer.getClass().getMethod("cleaner");
                cleaner.setAccessible(true);
                Method clean = Class.forName("sun.misc.Cleaner").getMethod("clean");
                clean.setAccessible(true);
                clean.invoke(cleaner.invoke(buffer));
            } else {
                Class<?> unsafeClass;
                try {
                    unsafeClass = Class.forName("sun.misc.Unsafe");
                } catch (Exception ex) {
                    // jdk.internal.misc.Unsafe doesn't yet have an invokeCleaner() method,
                    // but that method should be added if sun.misc.Unsafe is removed.
                    unsafeClass = Class.forName("jdk.internal.misc.Unsafe");
                }
                Method clean = unsafeClass.getMethod("invokeCleaner", ByteBuffer.class);
                clean.setAccessible(true);
                Field theUnsafeField = unsafeClass.getDeclaredField("theUnsafe");
                theUnsafeField.setAccessible(true);
                Object theUnsafe = theUnsafeField.get(null);
                clean.invoke(theUnsafe, buffer);
            }
        } catch (Exception e) {
            logger.error("****** Can not run cleaner.clean() to unmap MappedByteBuffer: " + e.toString(), e);
        }

    }
}
