/*
 * Copyright (C) 2016 Simon Norberg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.awsomefox.sprocket.util;

import android.view.View;

public final class Views {

  private Views() {
    // no instances
  }

  public static void visible(View view) {
    if (view != null) {
      view.setVisibility(View.VISIBLE);
    }
  }

  public static void invisible(View view) {
    if (view != null) {
      view.setVisibility(View.INVISIBLE);
    }
  }

  public static void gone(View view) {
    if (view != null) {
      view.setVisibility(View.GONE);
    }
  }
}
