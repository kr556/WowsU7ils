package org.w7ls.common.utils;

import java.io.Serializable;

// minとmaxを返すときに使う一時変数
public record MinMax(double min, double max) implements Serializable {
}
