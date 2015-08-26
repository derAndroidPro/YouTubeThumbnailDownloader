package derandroidpro.youtubethumbnaildownloader;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

public class MainActivity extends AppCompatActivity {

    Button btn;
    EditText editText;
    ImageView imageView;
    CardView cardView;
    ProgressDialog progressDialog;
    File thumbnailfile;
    Menu publicmenu;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText = (EditText) findViewById(R.id.editText);
        imageView = (ImageView) findViewById(R.id.imageView);
        cardView = (CardView) findViewById(R.id.cardview);
        btn = (Button) findViewById(R.id.button);

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    btn.setEnabled(true);
                }
                if (s.length() == 0) {
                    btn.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }

        });
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProgressDialog();
                prepareThumbnailURL(editText.getText().toString().trim());
                editText.setText(null);
                View keyboardView = getCurrentFocus();
                if (keyboardView != null) {
                    InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(keyboardView.getWindowToken(), 0);
                }
            }
        });


      /*  else if(getIntent().getStringExtra(Intent.EXTRA_TEXT) == null) {
                Log.d("IntentNoExtra", "Intent hat kein Text_Extra");
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }   */

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("ON_RESUME", "ausgeführt!");
        if(thumbnailfile != null && ! thumbnailfile.exists()){
            imageView.setImageBitmap(null);
            cardView.setVisibility(View.GONE);
        }


        String urlFromIntent = null;
        if (getIntent().getStringExtra(Intent.EXTRA_TEXT) != null && getIntent().getAction().equals(Intent.ACTION_SEND)) {
            Log.d("IntentText: ", getIntent().getStringExtra(Intent.EXTRA_TEXT));
            urlFromIntent = getIntent().getStringExtra(Intent.EXTRA_TEXT);
            showProgressDialog();
            prepareThumbnailURL(urlFromIntent);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        getIntent().removeExtra(Intent.EXTRA_TEXT);
    }

    private void showProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.progress_dialog_message));
        progressDialog.setCancelable(false);
        progressDialog.show();
    }


    private void prepareThumbnailURL(String videourl) {
        final String thumbnailURL;
        final String videoID;

        if (videourl.contains("youtube.com/watch?v=")) {
            String[] videoURLSplit = videourl.split("=");
            String[] videoURLSplit_playlist_or_extension = videoURLSplit[1].split("&");
            videoID = videoURLSplit_playlist_or_extension[0];
            thumbnailURL = "https://i.ytimg.com/vi/" + videoURLSplit_playlist_or_extension[0] + "/maxresdefault.jpg";
        } else {
            Uri videouri = Uri.parse(videourl);
            videoID = videouri.getLastPathSegment();
            thumbnailURL = "https://i.ytimg.com/vi/" + videouri.getLastPathSegment() + "/maxresdefault.jpg";
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                downloadThumbnail(thumbnailURL, videoID);
            }
        }).start();
    }

    private void downloadThumbnail(String thumbnailURLstring, final String videoID) {
        try {
            URL url = new URL(thumbnailURLstring);
            URLConnection urlConnection = url.openConnection();
            urlConnection.connect();

            InputStream inputStream = new BufferedInputStream(url.openStream(), 8192);

            File ordnerpfad = new File(Environment.getExternalStorageDirectory(), "YouTubeThumbnailDownloader");
            if (!ordnerpfad.exists()) {
                ordnerpfad.mkdirs();
            }
            thumbnailfile = new File(ordnerpfad, "thumbnail_" + videoID + ".jpg");
            OutputStream outputStream = new FileOutputStream(thumbnailfile);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();


            final Bitmap thumbnailBitmap = BitmapFactory.decodeFile(thumbnailfile.getAbsolutePath());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    cardView.setVisibility(View.VISIBLE);
                    imageView.setImageBitmap(thumbnailBitmap);
                    progressDialog.dismiss();
                    setSharemenuItemVisible();
                }
            });

            addFileToMediaStore(thumbnailfile.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
            File thumbnailcheckfile = new File(Environment.getExternalStorageDirectory(), "YouTubeThumbnailDownloader" + "/thumbnail_" + videoID + ".jpg");
            if (!thumbnailcheckfile.exists()) {
                trySmallerResolutionDownload(videoID);
            } else if(thumbnailcheckfile.exists()) {

                try {
                    final Bitmap thumbnailCheckbitmap = BitmapFactory.decodeFile(thumbnailcheckfile.getAbsolutePath());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            cardView.setVisibility(View.VISIBLE);
                            imageView.setImageBitmap(thumbnailCheckbitmap);
                            progressDialog.dismiss();
                            setSharemenuItemVisible();
                        }
                    });
                } catch (Exception e2) {
                    Toast.makeText(getApplicationContext(), getString(R.string.download_fail),Toast.LENGTH_SHORT).show();
                }

            }
        }
    }

    // Diese Methode wird in Catch von "downloadThumnail()" ausgeführt, damit das Thumbnail in niedrigerer
    // Auflösung heruntergeladen wird, wenn das Thumbnail nicht in der höchsten Auflösung verfügbar ist.
    private void trySmallerResolutionDownload(String videoID) {
        try {
            URL url = new URL("https://i.ytimg.com/vi/" + videoID + "/hqdefault.jpg");
            URLConnection urlConnection = url.openConnection();
            urlConnection.connect();

            InputStream inputStream = new BufferedInputStream(url.openStream(), 8192);

            File ordnerpfad = new File(Environment.getExternalStorageDirectory(), "YouTubeThumbnailDownloader");
            if (!ordnerpfad.exists()) {
                ordnerpfad.mkdirs();
            }
            thumbnailfile = new File(ordnerpfad, "thumbnail_" + videoID + "_lowRes.jpg");
            OutputStream outputStream = new FileOutputStream(thumbnailfile);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();


            final Bitmap thumbnailBitmap = BitmapFactory.decodeFile(thumbnailfile.getAbsolutePath());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    cardView.setVisibility(View.VISIBLE);
                    imageView.setImageBitmap(thumbnailBitmap);
                    progressDialog.dismiss();
                    setSharemenuItemVisible();
                }
            });

           addFileToMediaStore(thumbnailfile.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.dismiss();
                    Toast.makeText(getApplicationContext(), (R.string.toast_invalid_url), Toast.LENGTH_LONG).show();
                    imageView.setImageBitmap(null);
                    cardView.setVisibility(View.GONE);

                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        publicmenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_recent_thumbnails_activity) {
            startActivity(new Intent(this, RecentThumbnailsActivity.class));
            return true;
        }

        if (id == R.id.action_open_youtube) {
            try {
                Intent startyoutubeIntent = getPackageManager().getLaunchIntentForPackage("com.google.android.youtube");
                startActivity(startyoutubeIntent);
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), getString(R.string.toast_yt_app_not_installed), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
            return true;
        }

        if (id == R.id.action_developer_info) {
            showDeveloperInfo();
            return true;
        }
        if (id == R.id.action_share_thumbnail) {
            Intent shareintent = new Intent();
            shareintent.setAction(Intent.ACTION_SEND);
            shareintent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(thumbnailfile));
            shareintent.setType("image/jpg");
            startActivity(Intent.createChooser(shareintent, getString(R.string.sharedialog_title)));
        }


        return super.onOptionsItemSelected(item);
    }

    private void showDeveloperInfo() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle(getString(R.string.action_developerinfo_menu));
        View dialogview = LayoutInflater.from(this).inflate(R.layout.dev_info_dialog_layout, null);
        if(Build.VERSION.SDK_INT <11){
            TextView dialogtv = (TextView) dialogview.findViewById(R.id.textView);
            dialogtv.setTextColor(Color.WHITE);
        }

        alertDialog.setView(dialogview);
        alertDialog.setPositiveButton(getString(R.string.dialogclose), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        alertDialog.show();
    }

    private void setSharemenuItemVisible() {
        publicmenu.findItem(R.id.action_share_thumbnail).setVisible(true);

    }

    private void addFileToMediaStore(String filepath){
        ContentValues thumbnailValues = new ContentValues();
        thumbnailValues.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        thumbnailValues.put(MediaStore.MediaColumns.DATA,filepath);
        ContentResolver contentResolver = getContentResolver();
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, thumbnailValues);

    }

}
