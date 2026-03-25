package com.sahan.app.pixelforge.adapters;

import android.icu.text.NumberFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sahan.app.pixelforge.R;
import com.sahan.app.pixelforge.models.Order;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.ViewHolder> {

    private final List<Order> orderList;
    private OnOrderClickListener clickListener;

    public OrdersAdapter(List<Order> orderList) {
        this.orderList = orderList;
    }

    public void setOnOrderClickListener(OnOrderClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order order = orderList.get(position);

        holder.orderIdText.setText("Order #" + order.getOrderId());
        holder.orderStatusText.setText(order.getStatus());

        NumberFormat formatter = NumberFormat.getNumberInstance();
        holder.orderTotalPrice.setText("LKR " + formatter.format(order.getTotalAmount()));

        if (order.getOrderDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
            String formattedDate = sdf.format(order.getOrderDate().toDate());
            holder.orderDateText.setText(formattedDate);
        } else {
            holder.orderDateText.setText("Date unavailable");
        }
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onOrderClick(order);
            }
        });
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView orderIdText;
        TextView orderDateText;
        TextView orderStatusText;
        TextView orderTotalPrice;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            orderIdText = itemView.findViewById(R.id.order_id_text);
            orderDateText = itemView.findViewById(R.id.order_date_text);
            orderStatusText = itemView.findViewById(R.id.order_status_text);
            orderTotalPrice = itemView.findViewById(R.id.order_total_price);
        }
    }

    public interface OnOrderClickListener {
        void onOrderClick(Order order);
    }
}