package com.salesforce.phoenix.cache.aggcache;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.common.io.Closeables;

/**
 * This class abstracts a SpillFile It is a accessible on a per page basis
 * For every SpillFile object a single spill file is always created. 
 * Additional overflow files are dynamically created in case the page index requested is not covered by
 * the spillFiles allocated so far
 */
public class SpillFile implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(SpillFile.class);
    // Default size for a single spillFile 2GB
    private static final int SPILL_FILE_SIZE = Integer.MAX_VALUE;
    // Page size for a spill file 4K
    static final int DEFAULT_PAGE_SIZE = 4096;
    // Map of initial SpillFile at index 0, and overflow spillFiles
    private Map<Integer, TempFile> tempFiles;
    
    // Wrapper class for a TempFile: File + RandomAccessFile
    private static class TempFile implements Closeable{
    	private RandomAccessFile rndFile;
    	private File file;
    	
    	public TempFile(File file, RandomAccessFile rndFile) {
    		this.file = file;
    		this.rndFile = rndFile;
    	}    	
    	    	
    	public FileChannel getChannel() {
    		return rndFile.getChannel();
    	}

		@Override
		public void close() throws IOException {
			Closeables.closeQuietly(rndFile.getChannel());
			Closeables.closeQuietly(rndFile);
			
			if (file != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Deleting tempFile: " + file.getAbsolutePath());
                }
                try {
                    file.delete();
                } catch (SecurityException e) {
                    logger.warn("IOException thrown while closing Closeable." + e);
            	}
            }
		}
    }
    
    /**
     * Create a new SpillFile using the Java TempFile creation function. SpillFile is access in
     * pages.
     */
    public static SpillFile createSpillFile() {
    	try {    		
    		return new SpillFile(createTempFile());    		
    	} catch (IOException ioe) {
        	throw new RuntimeException("Could not create Spillfile " + ioe);
        }
    }
    
    
    private static TempFile createTempFile() throws IOException {
        File tempFile = File.createTempFile(UUID.randomUUID().toString(), null);
        if (logger.isDebugEnabled()) {
            logger.debug("Creating new SpillFile: " + tempFile.getAbsolutePath());
        }
        RandomAccessFile file = new RandomAccessFile(tempFile, "rw");
        file.setLength(SPILL_FILE_SIZE);
        
        return new TempFile(tempFile, file);
    }

    
    private SpillFile(TempFile spFile) throws IOException {
        this.tempFiles = Maps.newHashMap();
        // Init the first pre-allocated spillFile
        tempFiles.put(0, spFile);
    }

    /**
     * Random access to a page of the current spill file
     * @param index
     */
    public MappedByteBuffer getPage(int index) {
        try {
        	TempFile tempFile = null;
        	int fileIndex = 0;
        	
            long offset = (long) index * (long) DEFAULT_PAGE_SIZE;            
            if(offset >= SPILL_FILE_SIZE) {
            	// Offset exceeds the first SpillFile size
            	// Get the index of the file that should contain the pageID
            	fileIndex = (int)(offset / SPILL_FILE_SIZE);
            	if(!tempFiles.containsKey(fileIndex)) {
            		// Dynamically add new spillFiles if directory grows beyond 
            		// max page ID.
            		tempFile = createTempFile();
            		tempFiles.put(fileIndex, tempFile);
            	}
            }
        	tempFile = tempFiles.get(fileIndex);
        	// Channel gets buffered in file object
        	FileChannel fc = tempFile.getChannel();

        	return fc.map(MapMode.READ_WRITE, offset, DEFAULT_PAGE_SIZE);
        } catch (IOException ioe) {
            // Close resource
            close();
            throw new RuntimeException("Could not get page at index: " + index);
        } catch (IllegalArgumentException iae) {
            // Close resource
            close();
            throw iae;
        }
    }

    @Override
    public void close() {
    	for(TempFile file : tempFiles.values()) {
            // Swallow IOExceptions
            Closeables.closeQuietly(file);
    	}
    }
}
