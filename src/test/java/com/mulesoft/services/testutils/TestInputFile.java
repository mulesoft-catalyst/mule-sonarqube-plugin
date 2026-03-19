package com.mulesoft.services.testutils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.fs.TextRange;

/**
 * Minimal {@link InputFile} implementation for unit tests.
 *
 * <p>SonarQube's public plugin API does not ship the internal test builders that some older tests
 * relied on. This class provides just enough behavior to create {@code XmlFile} fixtures for XPath
 * function tests.
 *
 * @version 1.1.0
 * @since 1.1.0
 * @see org.sonarsource.analyzer.commons.xml.XmlFile#create(org.sonar.api.batch.fs.InputFile)
 */
public final class TestInputFile implements InputFile {
	private final Path path;
	private final String relativePath;
	private final Charset charset;

	/**
	 * Creates a test input file wrapper around a real filesystem path.
	 *
	 * @param path path to the file on disk
	 * @param relativePath relative path string used as the Sonar component key
	 * @param charset charset used when reading file contents
	 */
	public TestInputFile(Path path, String relativePath, Charset charset) {
		this.path = path;
		this.relativePath = relativePath;
		this.charset = charset;
	}

	/**
	 * Returns the component key (uses the provided relative path).
	 *
	 * @return component key
	 */
	@Override
	public String key() {
		return relativePath;
	}

	/**
	 * Indicates this component represents a file.
	 *
	 * @return always {@code true}
	 */
	@Override
	public boolean isFile() {
		return true;
	}

	/**
	 * Returns the relative path for this file.
	 *
	 * @return relative path
	 */
	@Override
	public String relativePath() {
		return relativePath;
	}

	/**
	 * Returns the absolute path for this file.
	 *
	 * @return absolute path as a string
	 */
	@Override
	public String absolutePath() {
		return path.toAbsolutePath().toString();
	}

	/**
	 * Returns the file as a {@link File}.
	 *
	 * @return underlying file
	 */
	@Override
	public File file() {
		return path.toFile();
	}

	/**
	 * Returns the file path.
	 *
	 * @return underlying {@link Path}
	 */
	@Override
	public Path path() {
		return path;
	}

	/**
	 * Returns a URI for the file.
	 *
	 * @return file URI
	 */
	@Override
	public URI uri() {
		return path.toUri();
	}

	/**
	 * Returns the base filename.
	 *
	 * @return filename
	 */
	@Override
	public String filename() {
		Path name = path.getFileName();
		return name == null ? relativePath : name.toString();
	}

	/**
	 * Returns the language key for the file.
	 *
	 * <p>This test implementation does not set a language.
	 *
	 * @return null
	 */
	@Override
	public String language() {
		return null;
	}

	/**
	 * Returns the file type.
	 *
	 * @return {@link Type#MAIN}
	 */
	@Override
	public Type type() {
		return Type.MAIN;
	}

	/**
	 * Opens an {@link InputStream} to the file.
	 *
	 * @return input stream
	 * @throws IOException when the file cannot be opened
	 */
	@Override
	public InputStream inputStream() throws IOException {
		return Files.newInputStream(path);
	}

	/**
	 * Reads the full contents of the file.
	 *
	 * @return file contents as a string
	 * @throws IOException when the file cannot be read
	 */
	@Override
	public String contents() throws IOException {
		return Files.readString(path, charset);
	}

	/**
	 * Returns the (approximate) SCM status for this file.
	 *
	 * @return {@link Status#SAME}
	 */
	@Override
	public Status status() {
		return Status.SAME;
	}

	/**
	 * Counts the number of lines in the file.
	 *
	 * @return number of lines, or 0 if the file cannot be read
	 */
	@Override
	public int lines() {
		try {
			return (int) Files.lines(path, charset).count();
		} catch (IOException e) {
			return 0;
		}
	}

	/**
	 * Determines whether the file is empty.
	 *
	 * @return true when the file cannot be read or has size 0
	 */
	@Override
	public boolean isEmpty() {
		try {
			return Files.size(path) == 0L;
		} catch (IOException e) {
			return true;
		}
	}

	/**
	 * Not implemented for tests that only need {@link #inputStream()} and {@link #contents()}.
	 *
	 * @param line line number
	 * @param lineOffset column offset
	 * @return never returns normally
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public TextPointer newPointer(int line, int lineOffset) {
		throw new UnsupportedOperationException("Not needed for these unit tests");
	}

	/**
	 * Not implemented for these unit tests.
	 *
	 * @param start start pointer
	 * @param end end pointer
	 * @return never returns normally
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public TextRange newRange(TextPointer start, TextPointer end) {
		throw new UnsupportedOperationException("Not needed for these unit tests");
	}

	/**
	 * Not implemented for these unit tests.
	 *
	 * @param startLine start line
	 * @param startLineOffset start column
	 * @param endLine end line
	 * @param endLineOffset end column
	 * @return never returns normally
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public TextRange newRange(int startLine, int startLineOffset, int endLine, int endLineOffset) {
		throw new UnsupportedOperationException("Not needed for these unit tests");
	}

	/**
	 * Not implemented for these unit tests.
	 *
	 * @param line line number
	 * @return never returns normally
	 * @throws UnsupportedOperationException always
	 */
	@Override
	public TextRange selectLine(int line) {
		throw new UnsupportedOperationException("Not needed for these unit tests");
	}

	/**
	 * Returns the charset used to read file contents.
	 *
	 * @return charset
	 */
	@Override
	public Charset charset() {
		return charset;
	}

	/**
	 * Returns a debugging string representation.
	 *
	 * @return string representation
	 */
	@Override
	public String toString() {
		return "TestInputFile[" + relativePath + "]";
	}
}

