package aQute.bnd.main.testrules;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.rules.TemporaryFolder;

import aQute.lib.io.IO;

/**
 * The Class WatchedTemporaryFolder.
 */
public class WatchedTemporaryFolder extends TemporaryFolder implements WatchedFolder {

	private Map<Path, String>		snapshotData	= Collections.unmodifiableMap(new HashMap<>());

	private Function<Path, String>	snapshotFunc	= p -> "";

	public WatchedTemporaryFolder() {
		super(getParent());
	}

	private static File getParent() {
		File parent = new File("generated/tmp/test").getAbsoluteFile();
		try {
			IO.mkdirs(parent);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return parent;
	}

	@Override
	public Path getRootPath() {
		return getRoot().toPath();
	}

	@Override
	public WatchedFolder copyDataFrom(Path p) throws IOException {
		IO.copy(IO.getFile(p.toFile()
			.getAbsolutePath()), getRoot());

		return this;
	}

	@Override
	public void snapshot(Function<Path, String> func) throws RuntimeException {
		snapshot(func, 0);
	}

	@Override
	public void snapshot(Function<Path, String> func, long delay) throws RuntimeException {
		try {
		snapshotFunc = func;
		snapshotData = Collections.unmodifiableMap(Files.walk(getRootPath())
			.collect(Collectors.toMap((Path p) -> p, p -> func.apply(p))));
			Thread.sleep(delay);
		} catch (InterruptedException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public WatchedFolder.FileStatus checkFile(Path relPath) {
		return computeFileStatus(getRootPath().resolve(relPath));
	}

	@Override
	public File getFile(String relPath) {
		return getRootPath().resolve(relPath)
			.toFile();
	}

	@Override
	public void print(PrintStream printStream, boolean relativize) throws IOException {
		Files.walk(getRootPath())
			.forEach(p -> printStream.println(relativize ? getRootPath().relativize(p) : p));
	}

	@Override
	public Map<Path, FileStatus> createFileStatistic(boolean relativize) throws IOException {
		// walk over all files (created, modified, unmodified)
		Map<Path, FileStatus> result = Files.walk(getRootPath())
			.collect(Collectors.toMap((Path p) -> (relativize ? getRootPath().relativize(p) : p),
				p -> computeFileStatus(p)));

		// add deleted files
		Map<Path, FileStatus> deleted = snapshotData.keySet()
			.stream()
			.map(p -> getRootPath().relativize(p))
			.filter(p -> !result.containsKey(p))
			.collect(Collectors.toMap((Path p) -> p, p -> FileStatus.DELETED));

		result.putAll(deleted);

		return result;
	}

	private WatchedFolder.FileStatus computeFileStatus(final Path p) {
		final boolean snapshotFound = snapshotData.containsKey(p);

		if (Files.exists(p)) {
			if (!snapshotFound) {
				return FileStatus.CREATED;
			}

			return snapshotData.get(p)
				.equals(snapshotFunc.apply(p)) ? FileStatus.UNMODIFIED_EXISTS : FileStatus.MODIFIED;
		}

		return snapshotFound ? FileStatus.DELETED : FileStatus.UNMODIFIED_NOT_EXISTS;
	}
}