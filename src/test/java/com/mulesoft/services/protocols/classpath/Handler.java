package com.mulesoft.services.protocols.classpath;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * Test-only URL stream handler for the {@code classpath:} URL scheme.
 *
 * <p>The production code supports {@code classpath:...} specs (for example, when loading embedded
 * rule stores). The tests set {@code java.protocol.handler.pkgs=com.mulesoft.services.protocols},
 * and this handler resolves URLs via a {@link ClassLoader}.
 *
 * @version 1.1.0
 * @since 1.1.0
 * @see com.mulesoft.services.tools.validation.RuleFactory
 */
public class Handler extends URLStreamHandler {

	private final ClassLoader classLoader;

	/**
	 * Creates a handler using this class's classloader.
	 */
	public Handler() {
		this.classLoader = getClass().getClassLoader();
	}

	/**
	 * Creates a handler using the provided classloader.
	 *
	 * @param classLoader classloader used to resolve classpath resources
	 */
	public Handler(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/**
	 * Opens a connection to the resolved classpath resource.
	 *
	 * @param u URL whose path points to a classpath resource
	 * @return URL connection for the resolved resource
	 * @throws IOException when the resource cannot be resolved or opened
	 */
	@Override
	protected URLConnection openConnection(URL u) throws IOException {
		final URL resourceUrl = classLoader.getResource(u.getPath());
		return resourceUrl.openConnection();
	}

}
