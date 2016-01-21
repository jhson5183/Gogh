package com.jhson.imageload.imageloader;

public class GoghOptions {

	private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
	public static final int NETWORK_POOL_SIZE = CPU_COUNT * 2 + 1;
	public static final int CACHE_POOL_SIZE = 1;
	
}
