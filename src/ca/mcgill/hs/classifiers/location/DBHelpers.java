package ca.mcgill.hs.classifiers.location;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public final class DBHelpers {

	/** Fast & simple file copy. */
	public static void copy(final File source, final File dest)
			throws IOException {
		FileChannel in = null, out = null;
		try {
			in = new FileInputStream(source).getChannel();
			out = new FileOutputStream(dest).getChannel();

			final long size = in.size();
			final MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY,
					0, size);

			out.write(buf);

		} finally {
			if (in != null) {
				in.close();
			}
			if (out != null) {
				out.close();
			}
		}
	}

	/**
	 * Class only contains static helper methods, and should not be
	 * instantiated.
	 */
	private DBHelpers() {
	}

}
