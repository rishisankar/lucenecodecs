package examples;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.codecs.PostingsFormat;
import org.apache.lucene.codecs.lucene80.Lucene80Codec;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.rsankar.lucenecodecs.mapcodec.MapPostingsFormat;

/**
 * Index all text files under a directory. The first paragraph is indexed with
 * field name "para1", the second paragraph with field name "para2", and the
 * rest with field name "rest".
 *
 * Run it with no command-line arguments for usage information.
 */
public final class IndexFiles {

	public static void main(String[] args) {
		String usage = "java lucenetest.IndexFiles" + " [-index INDEX_PATH] [-docs DOCS_PATH]\n\n"
				+ "Indexes the files in DOCS_PATH, creating a Lucene index in INDEX_PATH. Each file "
				+ "is a separate document 0 - its contents indexed as three fields (para1, para2, and " + "rest).\n\n"
				+ "The index can be searched with lucenetest.SearchFiles";
		String indexPath = "index";
		String docsPath = null;
		for (int i = 0; i < args.length; ++i) {
			if ("-index".equals(args[i])) {
				indexPath = args[++i];
			} else if ("-docs".equals(args[i])) {
				docsPath = args[++i];
			}
		}
		if (docsPath == null) {
			System.err.println("Usage: " + usage);
			System.exit(1);
		}
		final Path docDir = Paths.get(docsPath);
		if (!Files.isReadable(docDir)) {
			System.out.println("Document directory '" + docDir.toAbsolutePath()
					+ "' does not exist or is not readable, please check the path");
			System.exit(1);
		}
		try {
			System.out.println("Indexing to directory '" + indexPath + "'...");

			// FSDirectory.open() chooses the best directory implementation - which is
			// MMapDirectory for MacOS
			Directory dir = FSDirectory.open(Paths.get(indexPath));
			Analyzer analyzer = new StandardAnalyzer();
			IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
			iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
			iwc.setUseCompoundFile(false);

			iwc.setCodec(new Lucene80Codec() {
				@Override
				public PostingsFormat getPostingsFormatForField(String field) {
					return new MapPostingsFormat();
					/*if (field.equals("para1"))
						return new Lucene50PostingsFormat();
					else if (field.equals("para2"))
						/eturn new SimpleTextPostingsFormat();
					else
						return new Lucene50PostingsFormat(); */
				}
			});

			IndexWriter writer = new IndexWriter(dir, iwc);
			indexDocs(writer, docDir);
			// Merge all segments into one
			writer.forceMerge(1);
			writer.close();
		} catch (IOException e) {
			System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
		}
	}

	/**
	 * If path is a file, it indexes it, if it is a directory, it indexes all files
	 * contained within the directory.
	 */
	static void indexDocs(IndexWriter writer, Path path) throws IOException {
		if (Files.isDirectory(path)) {
			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					try {
						if (!file.getFileName().toString().equals("README.md"))
							indexDoc(writer, file);
					} catch (IOException ignore) {
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} else {
			indexDoc(writer, path);
		}
	}

	static String getNextParagraph(BufferedReader reader) throws IOException {
		String result = reader.readLine();
		if (result == null) {
			return null;
		}
		for (;;) {
			String nextLine = reader.readLine();
			if (nextLine == null || nextLine.length() == 0) {
				return result;
			}
			result += " " + nextLine;
		}
	}

	/**
	 * Indexes a single document.
	 */
	static void indexDoc(IndexWriter writer, Path file) throws IOException {
		try (InputStream stream = Files.newInputStream(file)) {
			Document doc = new Document();

			// Add the path of the file to the index
			Field pathField = new StringField("path", file.toString(), Field.Store.YES);
			doc.add(pathField);

			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			String fieldName = "para1";

			for (;;) {
				String para = getNextParagraph(reader);
				if (para == null) {
					break;
				}
				doc.add(new TextField(fieldName, para, Field.Store.NO));
				if (fieldName.equals("para1")) {
					fieldName = "para2";
				} else if (fieldName.equals("para2")) {
					fieldName = "rest";
				}
			}
			System.out.println("Adding " + file + " . . .");
			writer.addDocument(doc);
		}
	}
}
