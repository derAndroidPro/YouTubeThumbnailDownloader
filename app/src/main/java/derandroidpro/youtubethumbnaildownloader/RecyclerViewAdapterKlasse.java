package derandroidpro.youtubethumbnaildownloader;


import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


import java.io.File;

public class RecyclerViewAdapterKlasse extends RecyclerView.Adapter<RecyclerViewAdapterKlasse.ViewHolderKlasse> {

    public class ViewHolderKlasse extends RecyclerView.ViewHolder{
        ImageView itemImageView;

        public ViewHolderKlasse(View itemView) {
            super(itemView);
            itemImageView = (ImageView) itemView.findViewById(R.id.itemImageView);
        }
    }

    @Override
    public ViewHolderKlasse onCreateViewHolder(ViewGroup viewGroup, int i) {
        View itemView1 = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.recycler_view_item_layout1, null);
        return new ViewHolderKlasse(itemView1);
    }

    @Override
    public void onBindViewHolder(ViewHolderKlasse viewHolderKlasse, final int i) {
        viewHolderKlasse.itemImageView.setImageBitmap(RecentThumbnailsActivity.thumbnailbitmaps.get(i));
        viewHolderKlasse.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showActionsDialog(i, v.getContext());
            }
        });
    }

    @Override
    public int getItemCount() {
        return RecentThumbnailsActivity.thumbnailbitmaps.size();
    }


    private void showActionsDialog(final int position, final Context context){



        final AlertDialog alertdialog = new AlertDialog.Builder(context).create();

        View dialogView = LayoutInflater.from(context).inflate(R.layout.view_thumbnail_dialog_layout, null);
        alertdialog.setView(dialogView);
        ImageView dialogImageView = (ImageView)dialogView.findViewById(R.id.imageViewViewThumbnail);
        RelativeLayout sharebutton = (RelativeLayout) dialogView.findViewById(R.id.buttonShare_Layout);
        RelativeLayout deletebutton = (RelativeLayout) dialogView.findViewById(R.id.buttonDelete_Layout);

        if(Build.VERSION.SDK_INT <11){
            TextView dialogshareTv = (TextView) dialogView.findViewById(R.id.dialogShareTv);
            dialogshareTv.setTextColor(Color.WHITE);
            TextView dialogdeleteTv = (TextView) dialogView.findViewById(R.id.dialogDeleteTv);
            dialogdeleteTv.setTextColor(Color.WHITE);
        }

        final File selectedFile = RecentThumbnailsActivity.thumbnailfiles.get(position);
        Bitmap dialogImageBitmap = BitmapFactory.decodeFile(selectedFile.getAbsolutePath());
        dialogImageView.setImageBitmap(dialogImageBitmap);

        sharebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertdialog.dismiss();
                Intent shareintent = new Intent();
                shareintent.setAction(Intent.ACTION_SEND);
                shareintent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(selectedFile));
                shareintent.setType("image/jpg");
                context.startActivity(Intent.createChooser(shareintent, context.getString(R.string.sharedialog_title)));

            }
        });

        deletebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertdialog.dismiss();
                selectedFile.delete();
                RecentThumbnailsActivity.thumbnailfiles.remove(position);
                RecentThumbnailsActivity.thumbnailbitmaps.remove(position);
                notifyDataSetChanged();
                if(RecentThumbnailsActivity.thumbnailfiles.size() == 0){
                    RecentThumbnailsActivity.infotextView.setVisibility(View.VISIBLE);
                }

                ContentResolver contentResolver = context.getContentResolver();
                contentResolver.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.MediaColumns.DATA + "='" + selectedFile.getAbsolutePath() + "'",null);

                Toast.makeText(context, context.getString(R.string.toast_deleteconfirmation), Toast.LENGTH_SHORT).show();
            }
        });

        alertdialog.setButton(DialogInterface.BUTTON_POSITIVE, context.getString(R.string.dialogclose), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //nothing
            }
        });

        alertdialog.show();
    }


}
