package com.createchance.demo.views;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.createchance.demo.R;
import com.createchance.demo.model.Work;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * ${DESC}
 *
 * @author createchance
 * @date 2018/8/18
 */
public class WorksListAdapter extends RecyclerView.Adapter<WorksListAdapter.ViewHolder> {

    private Context mContext;

    private List<Work> mWorksList;

    public WorksListAdapter(Context context, List<Work> workList) {
        this.mContext = context;
        mWorksList = workList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(mContext).inflate(R.layout.list_item_works, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Work work = mWorksList.get(position);
        Glide.with(mContext).load(work.video).into(holder.thumbnail);
        holder.title.setText(work.title);
        holder.duration.setText(getFormattedDuration(work.duration));
        holder.createTime.setText(getFormattedTime(work.createTime));
    }

    @Override
    public int getItemCount() {
        return mWorksList.size();
    }

    private String getFormattedTime(long time) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
                "YYYY-MM-dd HH:mm:ss",
                Locale.getDefault());
        return simpleDateFormat.format(new Date(time));
    }

    private String getFormattedDuration(long duration) {
        int min = (int) (duration / (1000 * 60));
        int sec = (int) (duration % (1000 * 60));

        return min + ":" + sec;
    }

    static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        ImageView thumbnail;
        TextView duration;
        TextView title;
        TextView createTime;

        ViewHolder(View itemView) {
            super(itemView);

            thumbnail = itemView.findViewById(R.id.iv_works_thumbnail);
            itemView.findViewById(R.id.iv_works_play).setOnClickListener(this);
            duration = itemView.findViewById(R.id.tv_works_duration);
            title = itemView.findViewById(R.id.tv_works_title);
            createTime = itemView.findViewById(R.id.tv_works_create_time);
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.iv_works_play:
                    // play video.
                    break;
                default:
                    break;
            }
        }
    }
}
