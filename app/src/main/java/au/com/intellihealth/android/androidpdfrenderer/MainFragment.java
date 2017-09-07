package au.com.intellihealth.android.androidpdfrenderer;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class MainFragment extends FragmentActivity implements PdfPagingFragment.OnFragmentInteractionListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_fragment);
    }


    @Override
    public void onFragmentInteraction(Uri uri) {
        // Nothing to do...
    }
}
