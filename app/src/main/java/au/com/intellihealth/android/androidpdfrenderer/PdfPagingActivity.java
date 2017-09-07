package au.com.intellihealth.android.androidpdfrenderer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;

import static android.R.attr.orientation;

public class PdfPagingActivity extends AppCompatActivity {
    private static final int PDF_PAGE_PADDING = 5;
    private static final int PORTRAIT = 0, LANDSCAPE = 1;
    private static final String TAG = "PDF2";
    private ImageViewTouch imageView;
    private int maxPdfWidth = 0, maxPdfHeight = 0;
    private int maxScaledWidth = 0, maxScaledHeight = 0;
    private float screenWidthRatio = 1.0f;
    private int totalPdfHeight = 0;
    private int currentPage = 0, imageWidth, imageHeight, orientation = PORTRAIT;
    private Button previous, next;
    private PdfRenderer renderer;
    private String srcPdfFilename = "test.pdf";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_paging);

        imageView = (ImageViewTouch) findViewById(R.id.imagepdf);

        previous = (Button) findViewById(R.id.pdfPrevious);
        next = (Button) findViewById(R.id.pdfNext);

        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentPage++;
                render();
            }
        });
        previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentPage--;
                render();
            }
        });

        imageView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                // Ensure you call it only once :
                imageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                // Here you can get the size :)
                imageWidth = imageView.getWidth();
                //getResources().getDisplayMetrics().density;
                imageHeight = imageView.getHeight();

                if (imageWidth > imageHeight) {
                    orientation = LANDSCAPE;
                }

                preparePDF();
                render();
            }
        });

    }

    private void preparePDF() {
        try {
            File file = null;
            File folder = null;
            if (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).canRead()) {
                folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            } else if (Environment.getDownloadCacheDirectory().canRead()) {
                folder = Environment.getDownloadCacheDirectory();
            } else {
                folder = Environment.getRootDirectory();
            }
            Log.i(TAG, "PDF Source Folder: " + folder.toString());

            file = new File(folder, srcPdfFilename);
            Log.i(TAG, "PDF Source file: " + folder.toString());


            renderer = new PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY));
            int pdfPageHeight = 0, pdfPageWidth = 0;
            for (int i = 0; i < renderer.getPageCount(); i++) {
                PdfRenderer.Page pdfPage = renderer.openPage(i);
                pdfPageHeight = pdfPage.getHeight();
                pdfPageWidth = pdfPage.getWidth();

                if (pdfPageWidth > maxPdfWidth) {
                    //Log.i(TAG, "Current (w/h): "+maxPdfWidth+"/"+maxPdfHeight+" | new: "+pdfPageWidth+"/"+pdfPageHeight);
                    maxPdfWidth = pdfPageWidth;
                    maxPdfHeight = pdfPageHeight;
                }
                totalPdfHeight += pdfPageHeight;

                pdfPage.close();
            }
            // Adding the padding between the pages
            totalPdfHeight += (renderer.getPageCount() - 1) * PDF_PAGE_PADDING;
            // Calculates the screen width ratio (we're scrolling vertically)
            if (orientation == PORTRAIT) {
                Log.i(TAG, "In PORTRAIT");
                screenWidthRatio = (float) imageWidth / maxPdfWidth;
            } else {
                Log.i(TAG, "In LANDSCAPE");
                screenWidthRatio = (float) imageHeight / maxPdfHeight;
            }
            Log.i(TAG, "Max PDF Height: " + maxPdfHeight + " | max PDF Width: " + maxPdfWidth + " | total PDF Height: " + totalPdfHeight + " | screenWidthRatio:" + screenWidthRatio);
            maxScaledWidth = (int) (maxPdfWidth * screenWidthRatio);
            maxScaledHeight = (int) (maxPdfHeight * screenWidthRatio);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void render() {
        Log.i("PDF", "render (w/h)---  " + imageWidth + "x" + imageHeight);

        try {
            if (imageWidth > 0 && imageHeight > 0) {
                if (currentPage < 0) {
                    currentPage = 0;
                } else if (currentPage > renderer.getPageCount()-1) {
                    currentPage = renderer.getPageCount() - 1;
                }
                //TODO don't render if we're already on the last page... (disable next/previous button)

                Log.i(TAG, "maxPdfHeight: " + maxPdfHeight + " | maxPdfWidth: " + maxPdfWidth);
                Log.i(TAG, "screenHeight: " + imageHeight + " | screenWidth: " + imageWidth);
                Log.i(TAG, "totalPdfHeight: " + totalPdfHeight);

                // We still need a bitmap to convert the PDF page
                int pdfPageHeight = 0, pdfPageWidth = 0;
                float topOffset = 0f, leftOffset = 0f;
                float scale = 1f;

                Log.i(TAG, "----------------------- Page " + currentPage + " -------------------------");
                PdfRenderer.Page pdfPage = renderer.openPage(currentPage);
                pdfPageHeight = pdfPage.getHeight();
                pdfPageWidth = pdfPage.getWidth();
                Log.i(TAG, "pdfPageHeight: " + pdfPageHeight + " | pdfPageWidth: " + pdfPageWidth);

                int density = getResources().getDisplayMetrics().densityDpi;
                Log.i(TAG, "density: "+density);
                pdfPageWidth = density * pdfPageWidth / 150;
                pdfPageHeight = density * pdfPageHeight / 150;

                /*
                // Calculate ratio maxHeight/pageHeight
                if (orientation == PORTRAIT) {
                    scale = (float) pdfPageHeight / maxPdfHeight;
                } else {
                    scale = (float) pdfPageWidth / maxPdfWidth;
                }
                Log.i(TAG, "Scale: " + scale);

                // scale width based on ratio
                pdfPageWidth = (int) (pdfPageWidth * scale);
                pdfPageHeight = (int) (pdfPageHeight * scale);
                Log.i(TAG, "pdfPageHeight: " + pdfPageHeight + " | pdfPageWidth: " + pdfPageWidth);

                // Scaling the entire page
                pdfPageWidth = (int) (pdfPageWidth * screenWidthRatio);
                pdfPageHeight = (int) (pdfPageHeight * screenWidthRatio);
                Log.i(TAG, "pdfPageHeight: " + pdfPageHeight + " | pdfPageWidth: " + pdfPageWidth);
*/
                // All page should have the same height
                Bitmap currentPageBitmap = Bitmap.createBitmap(pdfPageWidth, pdfPageHeight, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(currentPageBitmap);
                canvas.drawColor(Color.WHITE);
                canvas.drawBitmap(currentPageBitmap, 0, 0, null);

                pdfPage.render(currentPageBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

                imageView.setImageBitmap(currentPageBitmap);
                imageView.invalidate();
                pdfPage.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
