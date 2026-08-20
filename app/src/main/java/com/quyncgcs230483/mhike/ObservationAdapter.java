package com.quyncgcs230483.mhike;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ObservationAdapter
    extends RecyclerView.Adapter<ObservationAdapter.ObservationViewHolder> {

  private List<Observation> observationList;
  private OnItemLongClickListener longClickListener;

  public interface OnItemLongClickListener {
    void onItemLongClick(Observation observation);
  }

  public ObservationAdapter(
      List<Observation> observationList, OnItemLongClickListener longClickListener) {
    this.observationList = observationList;
    this.longClickListener = longClickListener;
  }

  @NonNull
  @Override
  public ObservationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view =
        LayoutInflater.from(parent.getContext()).inflate(R.layout.item_observation, parent, false);
    return new ObservationViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull ObservationViewHolder holder, int position) {
    Observation observation = observationList.get(position);
    holder.tvName.setText(observation.getObservation());
    holder.tvTime.setText(observation.getTime());
    holder.tvComment.setText(observation.getComments());

    if (observation.getImagePath() != null && !observation.getImagePath().isEmpty()) {
      try {
        byte[] decodedString = Base64.decode(observation.getImagePath(), Base64.DEFAULT);
        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        holder.ivImage.setImageBitmap(decodedByte);
      } catch (Exception e) {
        holder.ivImage.setImageResource(android.R.drawable.ic_menu_gallery);
      }
    } else {
      holder.ivImage.setImageResource(android.R.drawable.ic_menu_gallery);
    }

    holder.itemView.setOnLongClickListener(
        new View.OnLongClickListener() {
          @Override
          public boolean onLongClick(View v) {
            if (longClickListener != null) {
              longClickListener.onItemLongClick(observation);
            }
            return true;
          }
        });
  }

  @Override
  public int getItemCount() {
    return observationList.size();
  }

  public static class ObservationViewHolder extends RecyclerView.ViewHolder {
    TextView tvName, tvTime, tvComment;
    ImageView ivImage;

    public ObservationViewHolder(@NonNull View itemView) {
      super(itemView);
      tvName = itemView.findViewById(R.id.tvObsName);
      tvTime = itemView.findViewById(R.id.tvObsTime);
      tvComment = itemView.findViewById(R.id.tvObsComment);
      ivImage = itemView.findViewById(R.id.ivObsImage);
    }
  }
}
