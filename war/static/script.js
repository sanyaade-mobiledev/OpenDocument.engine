/*
 * Copyright (c) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

var onSuccess = function(data, result, xhr) {
  if (data.redirect) {
    window.location.href = data.redirect;
  } else {
    document.getElementById("heading").innerText = data['title'];
    document.title += ' - ' + data['title'];
    document.getElementById('content').innerHTML = data['content'];
  }
};

var onError = function(xhr, text_status, error) {
  if (xhr.status == 302) {
	  document.location.href = xhr.getResponseHeader('Location');
  }
};

var get = function(file_id) {
  $.ajax({
      url: '/svc?file_id=' + file_id,
      success: onSuccess,
      error: onError
  });
};