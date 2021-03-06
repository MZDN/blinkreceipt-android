package com.blinkreceipt.ocr.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.blinkreceipt.ocr.R;
import com.blinkreceipt.ocr.Utility;
import com.blinkreceipt.ocr.adapter.ProductsAdapter;
import com.blinkreceipt.ocr.presenter.MainPresenter;
import com.blinkreceipt.ocr.transfer.RecognizeItem;
import com.microblink.CameraOrientation;
import com.microblink.Product;
import com.microblink.ReceiptSdk;
import com.microblink.ScanResults;

import java.util.List;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MainActivity extends AppCompatActivity {

    private static final int GALLERY_IMAGE_REQUEST = 1984;

    private static final String TAG = "MainActivity";

    private MainViewModel viewModel;

    private MainPresenter presenter;

    private ProgressBar progressBar;

    private ProductsAdapter adapter;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.activity_main );

        viewModel = ViewModelProviders.of( this ).get( MainViewModel.class );

        presenter = new MainPresenter();

        final RecyclerView recyclerView = findViewById( R.id.products );

        progressBar = findViewById( R.id.progressBar );

        adapter = new ProductsAdapter();

        LinearLayoutManager manager = new LinearLayoutManager( this );

        recyclerView.addItemDecoration( new DividerItemDecoration(this, manager.getOrientation() ) );

        recyclerView.setLayoutManager( manager );

        recyclerView.setAdapter( adapter );

        viewModel.results().observe(this, data -> {
            hideProgress();

            if ( data != null ) {
                Throwable e = data.e();

                if ( e != null ) {
                    Toast.makeText( MainActivity.this, e.toString(), Toast.LENGTH_LONG ).show();

                    return;
                }

                ScanResults results = data.results();

                if ( results == null ) {
                    Toast.makeText( MainActivity.this, R.string.no_products_found_on_receipt, Toast.LENGTH_SHORT ).show();

                    return;
                }

                List<Product> products = presenter.products( results );

                if ( !Utility.isNullOrEmpty( products ) ) {
                    adapter.addAll( products );

                    return;
                }
            }

            Toast.makeText( MainActivity.this, R.string.no_products_found_on_receipt, Toast.LENGTH_SHORT ).show();
        });

        viewModel.bitmap().observe(this, bitmap -> {
            if ( bitmap == null ) {
                hideProgress();

                Toast.makeText( MainActivity.this, R.string.bitmap_not_found, Toast.LENGTH_LONG ).show();

                return;
            }

            viewModel.recognizeItem( new RecognizeItem( viewModel.scanOptions(),
                    bitmap, CameraOrientation.ORIENTATION_PORTRAIT ) );
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();

        inflater.inflate( R.menu.main_menu, menu );

        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item ) {
        switch( item.getItemId() ) {
            case R.id.sdk_version:
                new AlertDialog.Builder( this )
                        .setTitle( R.string.sdk_version_dialog_title )
                        .setMessage( ReceiptSdk.versionName() )
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                        .create()
                        .show();

                return true;
            case R.id.camera:
                startActivityForResult( Intent.createChooser( new Intent()
                                .setType( "image/*" )
                                .setAction( Intent.ACTION_GET_CONTENT ),
                        getString( R.string.gallery_picture_title ) ), GALLERY_IMAGE_REQUEST );

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch ( requestCode ) {
            case GALLERY_IMAGE_REQUEST:
                switch ( resultCode ) {
                    case Activity.RESULT_OK:
                        try {
                            if ( data == null ) {
                                Toast.makeText( this, R.string.gallery_picture_exception, Toast.LENGTH_LONG ).show();

                                return;
                            }

                            adapter.clear();

                            progressBar.setVisibility( View.VISIBLE );

                            Uri uri = data.getData();

                            if ( uri == null ) {
                                hideProgress();

                                Toast.makeText( this, R.string.gallery_picture_exception, Toast.LENGTH_LONG ).show();

                                return;
                            }

                            viewModel.uri( uri );
                        } catch ( Throwable e ) {
                            Log.e( TAG, e.toString() );

                            Toast.makeText( this, e.toString(), Toast.LENGTH_LONG ).show();

                            hideProgress();
                        }

                        break;
                }

                break;
        }
    }

    private void hideProgress() {
        progressBar.setVisibility( View.GONE );
    }
}
