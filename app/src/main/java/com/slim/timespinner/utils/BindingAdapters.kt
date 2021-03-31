package com.slim.timespinner.utils

import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import com.slim.timespinner.ui.picker.NumberPicker

object BindingAdapters {

    @BindingAdapter("app:np_value")
    @JvmStatic
    fun setValue(view: NumberPicker, value: Int) {
        if (view.value != value) {
            view.value = value
        }
    }

    @InverseBindingAdapter(attribute = "app:np_value")
    @JvmStatic
    fun getValue(view: NumberPicker) = view.value

    @BindingAdapter(value = ["app:np_onValueChange", "app:np_valueAttrChanged"], requireAll = false)
    @JvmStatic
    fun setListeners(
        view: NumberPicker,
        listener: NumberPicker.OnValueChangeListener?,
        attrChange: InverseBindingListener?
    ) {
        if (attrChange == null) {
            view.setOnValueChangedListener(listener)
        } else {
            view.setOnValueChangedListener { picker, oldVal, newVal ->
                listener?.onValueChange(picker, oldVal, newVal)
                attrChange.onChange()
            }
        }
    }

}