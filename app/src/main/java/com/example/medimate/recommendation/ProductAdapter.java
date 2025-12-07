package com.example.medimate.recommendation;

import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.medimate.R;
import com.example.medimate.recommendation.api.NaverItem;
import java.util.ArrayList;
import java.util.List;
import android.content.Intent;
import android.net.Uri;

// 1. 아이템 타입 인터페이스
interface DisplayableItem {}

// 2. 헤더 아이템
class HeaderItem implements DisplayableItem {
    final String title;
    HeaderItem(String title) {
        this.title = title;
    }
}

// 3. 네이버 쇼핑 아이템
class NaverProductItem implements DisplayableItem {
    final NaverItem item;
    NaverProductItem(NaverItem item) {
        this.item = item;
    }
}

public class ProductAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // 뷰 타입 상수 (일반 ITEM 타입 삭제)
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_NAVER = 1;

    private final List<DisplayableItem> items = new ArrayList<>();

    // 1. 헤더 뷰홀더
    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        public TextView headerTitle;
        public HeaderViewHolder(View view) {
            super(view);
            headerTitle = view.findViewById(R.id.tvSectionHeader);
        }
    }
    // 2. 네이버 쇼핑 뷰홀더
    public static class NaverViewHolder extends RecyclerView.ViewHolder {
        public ImageView ivThumb;
        public TextView tvTitle;
        public TextView tvPrice;

        public NaverViewHolder(View view) {
            super(view);
            ivThumb = view.findViewById(R.id.ivThumb);
            tvTitle = view.findViewById(R.id.tvTitle);
            tvPrice = view.findViewById(R.id.tvPrice);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (items.get(position) instanceof HeaderItem) {
            return VIEW_TYPE_HEADER;
        } else {
            return VIEW_TYPE_NAVER; // 나머지는 무조건 네이버 아이템
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_section_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            // VIEW_TYPE_NAVER
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_naver_product, parent, false);
            return new NaverViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            HeaderItem headerItem = (HeaderItem) items.get(position);
            ((HeaderViewHolder) holder).headerTitle.setText(headerItem.title);

        } else if (holder instanceof NaverViewHolder) {
            NaverProductItem itemWrapper = (NaverProductItem) items.get(position);
            NaverItem item = itemWrapper.item;
            NaverViewHolder vh = (NaverViewHolder) holder;

            vh.tvTitle.setText(Html.fromHtml(item.title, Html.FROM_HTML_MODE_LEGACY));
            vh.tvPrice.setText(formatPrice(item.lprice) + "원");

            Glide.with(vh.itemView.getContext())
                    .load(item.image)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(vh.ivThumb);

            vh.itemView.setOnClickListener(v -> {
                if (item.link != null && !item.link.isEmpty()) {
                    // 인터넷 브라우저로 링크 열기
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.link));
                    v.getContext().startActivity(intent);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setItems(List<DisplayableItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    private String formatPrice(String price) {
        try {
            int p = Integer.parseInt(price);
            return String.format("%,d", p);
        } catch (NumberFormatException e) {
            return price;
        }
    }
}