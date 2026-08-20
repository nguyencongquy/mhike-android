package com.quyncgcs230483.mhike;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class HikeAdapter extends RecyclerView.Adapter<HikeAdapter.HikeViewHolder> {

  private List<Hike> hikeList;
  private final Context context;

  public HikeAdapter(List<Hike> hikeList, Context context) {
    this.hikeList = hikeList;
    this.context = context;
  }

  public void filterList(List<Hike> filteredList) {
    this.hikeList = filteredList;
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public HikeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view =
        LayoutInflater.from(parent.getContext()).inflate(R.layout.hike_list_item, parent, false);
    return new HikeViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull HikeViewHolder holder, int position) {
    Hike hike = hikeList.get(position);

    holder.tvName.setText(hike.getName());
    holder.tvLocation.setText("📍 " + hike.getLocation());
    holder.tvDate.setText("📅 " + hike.getDate());

    if (position % 2 == 0) {
      holder.cardView.setCardBackgroundColor(Color.parseColor("#FFFFFF"));
      holder.cardView.setStrokeColor(Color.parseColor("#4B5AE4"));
      holder.tvName.setTextColor(Color.parseColor("#4B5AE4"));
      holder.tvLocation.setTextColor(Color.parseColor("#4B5AE4"));
      holder.tvDate.setTextColor(Color.parseColor("#4B5AE4"));
    } else {
      holder.cardView.setCardBackgroundColor(Color.parseColor("#4B5AE4"));
      holder.cardView.setStrokeColor(Color.parseColor("#FCFDF2"));
      holder.tvName.setTextColor(Color.parseColor("#FCFDF2"));
      holder.tvLocation.setTextColor(Color.parseColor("#FCFDF2"));
      holder.tvDate.setTextColor(Color.parseColor("#FCFDF2"));
    }

    holder.itemView.setOnClickListener(
        v -> {
          Intent intent = new Intent(context, HikeDetailActivity.class);
          intent.putExtra("HIKE_ID", hike.getId());
          context.startActivity(intent);
        });
  }

  @Override
  public int getItemCount() {
    return hikeList.size();
  }

  public static class HikeViewHolder extends RecyclerView.ViewHolder {
    final TextView tvName, tvLocation, tvDate;
    final MaterialCardView cardView;

    public HikeViewHolder(@NonNull View itemView) {
      super(itemView);
      tvName = itemView.findViewById(R.id.tvItemHikeName);
      tvLocation = itemView.findViewById(R.id.tvItemLocation);
      tvDate = itemView.findViewById(R.id.tvItemDate);
      cardView = itemView.findViewById(R.id.cardViewItem);
    }
  }
}
