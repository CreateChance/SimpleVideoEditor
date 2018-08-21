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
import com.createchance.demo.model.Scene;
import com.createchance.demo.model.SimpleModelManager;
import com.createchance.demo.model.Template;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * ${DESC}
 *
 * @author createchance
 * @date 2018/8/19
 */
public class SceneListAdapter extends RecyclerView.Adapter<SceneListAdapter.ViewHolder> {

    private final Template mTemplate;
    private final Context mContext;
    private final boolean mIsCustom;
    private final VideoHandler mVideoHandler;

    private List<Scene> mSceneList = new ArrayList<>();

    private SimpleDialog mDeleteDialog;

    public SceneListAdapter(Context context, Template template, boolean isCustom, VideoHandler videoHandler) {
        this.mContext = context;
        this.mTemplate = template;
        this.mIsCustom = isCustom;
        this.mVideoHandler = videoHandler;
        getScenesFromTemplate();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(mContext).inflate(R.layout.list_item_scene, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Scene scene = mSceneList.get(position);

        holder.script.setText(scene.script);
        if (scene.video != null && scene.video.exists() && scene.video.isFile()) {
            holder.plus.setVisibility(View.GONE);
            holder.thumbnail.setVisibility(View.VISIBLE);
            Glide.with(mContext).load(scene.video).into(holder.thumbnail);
        } else {
            holder.plus.setVisibility(View.VISIBLE);
            holder.thumbnail.setVisibility(View.GONE);
        }

        holder.seq.setText(String.format(Locale.getDefault(), "%02d", (position + 1)));
    }

    @Override
    public int getItemCount() {
        return mSceneList.size();
    }

    private void getScenesFromTemplate() {
        if (mTemplate == null) {
            mSceneList.add(new Scene());
        } else {
            for (String script : mTemplate.scriptList) {
                Scene scene = new Scene();
                scene.script = script;
                mSceneList.add(scene);
            }
            SimpleModelManager.getInstance().setSceneList(mSceneList);
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        ImageView plus;
        ImageView thumbnail;
        TextView script;
        TextView seq;

        ViewHolder(View itemView) {
            super(itemView);

            plus = itemView.findViewById(R.id.iv_scene_plus);
            View delete = itemView.findViewById(R.id.iv_scene_delete);
            delete.setVisibility(View.VISIBLE);
            delete.setOnClickListener(this);
            thumbnail = itemView.findViewById(R.id.iv_scene_thumbnail);
            script = itemView.findViewById(R.id.tv_scene_script);
            seq = itemView.findViewById(R.id.tv_scene_seq);
            plus.setOnClickListener(this);
            thumbnail.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Scene scene = mSceneList.get(getAdapterPosition());
            switch (v.getId()) {
                case R.id.iv_scene_plus:
                    mVideoHandler.takeVideo(getAdapterPosition());
                    break;
                case R.id.iv_scene_delete:
                    if (mIsCustom) {
                        showDeleteSceneDialog(getAdapterPosition());
                    } else if (scene.video != null && scene.video.exists() && scene.video.isFile()) {
                        showDeleteVideoDialog(getAdapterPosition());
                    }
                    break;
                case R.id.iv_scene_thumbnail:
                    if (scene.video != null && scene.video.exists() && scene.video.isFile()) {
                        mVideoHandler.editVideo(getAdapterPosition());
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public interface VideoHandler {
        void takeVideo(int sceneIndex);

        void editVideo(int sceneIndex);
    }

    private void showDeleteSceneDialog(final int sceneIndex) {
        mDeleteDialog = new SimpleDialog.Builder(mContext)
                .setCancelTouchout(false)
                .setContentText(R.string.scene_delete_scene_dialog_content)
                .setCancelText(R.string.scene_delete_dialog_cancel)
                .setConfirmText(R.string.scene_delete_dialog_confirm)
                .setConfirmOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mSceneList.remove(sceneIndex);
                        notifyItemRemoved(sceneIndex);
                        mDeleteDialog.dismiss();
                    }
                }).build();
        mDeleteDialog.show();
    }

    private void showDeleteVideoDialog(final int sceneIndex) {
        mDeleteDialog = new SimpleDialog.Builder(mContext)
                .setCancelTouchout(false)
                .setContentText(R.string.scene_delete_video_dialog_content)
                .setCancelText(R.string.scene_delete_dialog_cancel)
                .setConfirmText(R.string.scene_delete_dialog_confirm)
                .setConfirmOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Scene scene = mSceneList.get(sceneIndex);
                        scene.video.delete();
                        scene.video = null;
                        notifyDataSetChanged();
                        mDeleteDialog.dismiss();
                    }
                }).build();
        mDeleteDialog.show();
    }
}
