package com.attentive.bonni.database

import androidx.annotation.DrawableRes
import com.attentive.bonni.R

/**
 * Product images, persisted by name. Drawable IDs change between builds, so a stored ID can
 * point at a different drawable after an update.
 */
enum class ProductImage(
    @DrawableRes val drawableRes: Int,
) {
    SUPERSCREEN(R.drawable.superscreen),
    STICK1(R.drawable.stick1),
    BALM2(R.drawable.balm2),
    BALM3(R.drawable.balm3),
}
