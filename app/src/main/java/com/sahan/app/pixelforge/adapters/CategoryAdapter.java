package com.sahan.app.pixelforge.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.sahan.app.pixelforge.R;
import com.sahan.app.pixelforge.models.Category;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    private final List<Category> categories;

    private OnCategoryClickListener clickListener;

    public CategoryAdapter(List<Category> categories, OnCategoryClickListener clickListener) {
        this.categories = categories;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = categories.get(position);
        holder.categoryName.setText(category.getCatName());
        Glide.with(holder.categoryImage.getContext()).load(category.getCatImageUrl()).centerCrop().into(holder.categoryImage);

        holder.itemView.setOnClickListener((View view) -> {
            Animation animation = AnimationUtils.loadAnimation(view.getContext(), R.anim.card_pressed_animation);
            view.startAnimation(animation);

            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (clickListener != null) {
                        clickListener.onCategoryClick(category);
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
        });

    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView categoryImage;
        TextView categoryName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryImage = itemView.findViewById(R.id.category_image);
            categoryName = itemView.findViewById(R.id.category_name);
        }
    }

    public interface OnCategoryClickListener{

        void onCategoryClick(Category category);

    }

}
