package org.rsankar.lucenecodecs.mapcodec;

import java.io.IOException;

import org.apache.lucene.codecs.FieldsConsumer;
import org.apache.lucene.codecs.FieldsProducer;
import org.apache.lucene.codecs.PostingsFormat;
import org.apache.lucene.codecs.PostingsReaderBase;
import org.apache.lucene.codecs.PostingsWriterBase;
import org.apache.lucene.codecs.lucene50.Lucene50PostingsReader;
import org.apache.lucene.codecs.lucene50.Lucene50PostingsWriter;
import org.apache.lucene.index.SegmentReadState;
import org.apache.lucene.index.SegmentWriteState;

public class MapPostingsFormat extends PostingsFormat {
	public static final String FIELD_MAP_EXTENSION = "fme"; 
	
	public MapPostingsFormat() {
		super("MapPostingsFormat");
	}

	@Override
	public FieldsConsumer fieldsConsumer(SegmentWriteState state) throws IOException {
		PostingsWriterBase pw = new Lucene50PostingsWriter(state);
		return new MapFieldsWriter(state, pw);
	}

	@Override
	public FieldsProducer fieldsProducer(SegmentReadState state) throws IOException {
		PostingsReaderBase pr = new Lucene50PostingsReader(state);
		return new MapFieldsReader(state, pr);
	}
	
	public static String getFieldMapFileName(String segmentName, String segmentSuffix) {
		return segmentName + "_" + segmentSuffix + "." + FIELD_MAP_EXTENSION;
	}
}
