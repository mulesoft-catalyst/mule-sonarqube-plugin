package com.mulesoft.services.protocols.classpath;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

public class Handler extends URLStreamHandler {

	private final ClassLoader classLoader;

	public Handler() {
		this.classLoader = getClass().getClassLoader();
	}

	public Handler(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	protected URLConnection openConnection(URL u) throws IOException {
		final URL resourceUrl = classLoader.getResource(u.getPath());
		return resourceUrl.openConnection();
	}

}
