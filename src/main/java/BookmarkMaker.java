import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitWidthDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;

import java.io.*;
import java.util.Stack;

/**
 * Created by Kevin Albertson on 6/11/2016.
 * A minimally simple pdf bookmarker.
 */
public class BookmarkMaker {
    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.err.println("Usage: java -jar BookmarkMaker.jar <pdf> <bookmark file> <page offset>");
            return;
        }
        String pdfFile = args[0];
        String bookmarkFile = args[1];
        int pageOffset = Integer.parseInt(args[2]);

        InputStreamReader fileInStream = new InputStreamReader(new FileInputStream(bookmarkFile));
        BufferedReader fileIn = new BufferedReader(fileInStream);
        PDDocument document = null;
        try
        {
            document = PDDocument.load(new File(pdfFile));
            if(document.isEncrypted()) {
                System.err.println("Error: Cannot add bookmarks to encrypted document.");
                System.exit( 1 );
            }

            PDDocumentOutline outline =  new PDDocumentOutline();
            document.getDocumentCatalog().setDocumentOutline(outline);

            Stack<PDOutlineItem> bookmarkStack = new Stack<PDOutlineItem>();
            int currentLevel = 0;
            String line;
            while ((line = fileIn.readLine()) != null) {
                int i = 0, pageNumber, nextLevel;
                String title, pageNumberStr = "";

                for (; i < line.length(); i++) {
                    if (line.charAt(i) != ' ') break;
                }
                nextLevel = i;
                for (; i < line.length(); i++) {
                    char letter = line.charAt(i);
                    int diff = letter - '0';
                    if (0 <= diff && diff <= 9) {
                        pageNumberStr += letter;
                    } else {
                        break;
                    }
                }
                pageNumber = Integer.parseInt(pageNumberStr);
                title = line.substring(i);

                PDPage page = document.getPage(pageNumber + pageOffset);
                PDPageFitWidthDestination dest = new PDPageFitWidthDestination();
                dest.setPage(page);
                PDOutlineItem bookmark = new PDOutlineItem();
                bookmark.setDestination(dest);
                bookmark.setTitle(title);

                if (nextLevel > currentLevel + 1) {
                    System.err.println("Malformed bookmark list, sub lists must be only one more than the parent.");
                    return;
                }

                // Pop the stack until we get the proper parent. The parent must be one level less than nextLevel.
                while (bookmarkStack.size() > 0 && bookmarkStack.size() > nextLevel) bookmarkStack.pop();
                if (bookmarkStack.size() == 0) {
                    outline.addLast(bookmark);
                } else {
                    bookmarkStack.peek().addLast(bookmark);
                }
                bookmarkStack.push(bookmark);
                currentLevel = nextLevel;
            }

            outline.openNode();
            document.save(pdfFile + ".with_bookmarks.pdf");
        }
        finally {
            if(document != null) {
                document.close();
            }
        }
    }
}
