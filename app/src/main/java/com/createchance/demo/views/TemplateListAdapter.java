package com.createchance.demo.views;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.createchance.demo.R;
import com.createchance.demo.model.Template;

import java.util.List;

/**
 * ${DESC}
 *
 * @author createchance
 * @date 2018/8/19
 */
public class TemplateListAdapter extends RecyclerView.Adapter<TemplateListAdapter.ViewHolder> {

    private Context mContext;

    private List<Template> mTemplateList;

    public TemplateListAdapter(Context context, List<Template> templateList) {
        this.mContext = context;
        this.mTemplateList = templateList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(mContext).inflate(R.layout.list_item_template, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Template template = mTemplateList.get(position);
        StringBuilder stringBuilder = new StringBuilder();
        for (String script : template.scriptList) {
            stringBuilder.append(script);
            stringBuilder.append("\n");
        }
        holder.scripts.setText(stringBuilder.toString());
    }

    @Override
    public int getItemCount() {
        return mTemplateList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView scripts;

        ViewHolder(View itemView) {
            super(itemView);

            itemView.findViewById(R.id.btn_apply).setOnClickListener(this);
            scripts = itemView.findViewById(R.id.tv_template_scripts);
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_apply:
                    ScenesActivity.start(
                            v.getContext(),
                            mTemplateList.get(getAdapterPosition()),
                            false);
                    break;
                default:
                    break;
            }
        }
    }
}
