package com.s23010749.nutriscan;

import com.google.gson.annotations.SerializedName;

public class ProductResponse {
    @SerializedName("status") private int status;
    @SerializedName("product") private Product product;

    public int getStatus() { return status; }
    public Product getProduct() { return product; }

    public static class Product {
        @SerializedName("product_name") private String productName;
        @SerializedName("nutriments") private Nutriments nutriments;

        public String getProductName() { return productName; }
        public Nutriments getNutriments() { return nutriments; }
    }

    public static class Nutriments {
        @SerializedName("sugars_100g") private double sugars100g;
        @SerializedName("fat_100g") private double fat100g;
        @SerializedName("salt_100g") private double salt100g;
        @SerializedName("proteins_100g") private double proteins100g;
        @SerializedName("carbohydrates_100g") private double carbohydrates100g;
        @SerializedName("energy-kcal_100g") private double energyKcal100g;

        public double getSugars100g() { return sugars100g; }
        public double getFat100g() { return fat100g; }
        public double getSalt100g() { return salt100g; }
        public double getProteins100g() { return proteins100g; }
        public double getCarbohydrates100g() { return carbohydrates100g; }
        public double getEnergyKcal100g() { return energyKcal100g; }
    }
}