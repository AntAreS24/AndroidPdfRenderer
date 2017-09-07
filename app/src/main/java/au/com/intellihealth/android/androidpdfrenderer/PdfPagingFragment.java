package au.com.intellihealth.android.androidpdfrenderer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;

import java.io.File;
import java.io.IOException;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;


/**
 */
public class PdfPagingFragment extends Fragment {
    private OnFragmentInteractionListener mListener;
    private static final int PDF_PAGE_PADDING = 5;
    private static final int PORTRAIT = 0, LANDSCAPE = 1;
    private static final String TAG = "PDF2";
    private static final String CURRENT_PAGE = "CURRENT_PAGE";
    private ImageViewTouch imageView;
    private int maxPdfWidth = 0, maxPdfHeight = 0;
    private int maxScaledWidth = 0, maxScaledHeight = 0;
    private float screenWidthRatio = 1.0f;
    private int totalPdfHeight = 0;
    private int currentPage = 0, imageWidth, imageHeight, orientation = PORTRAIT;
    private Button previous, next;
    private PdfRenderer renderer;
    private String srcPdfFilename = "test.pdf";

    public PdfPagingFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment PdfPagingFragment.
     */
    public static PdfPagingFragment newInstance() {
        PdfPagingFragment fragment = new PdfPagingFragment();
        Bundle args = new Bundle();
        //args.putString(ARG_PARAM1, param1);
        //args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            //mParam1 = getArguments().getString(ARG_PARAM1);
            //mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        imageView = getView().findViewById(R.id.imagepdf);

        previous = getView().findViewById(R.id.pdfPrevious);
        next = getView().findViewById(R.id.pdfNext);

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

        // Restore the page from the state (if any)
        if (savedInstanceState != null && savedInstanceState.containsKey(CURRENT_PAGE)) {
            currentPage = savedInstanceState.getInt(CURRENT_PAGE);
        }

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.activity_pdf_paging, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(CURRENT_PAGE, currentPage);
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
                } else if (currentPage > renderer.getPageCount() - 1) {
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
                Log.i(TAG, "density: " + density);
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
