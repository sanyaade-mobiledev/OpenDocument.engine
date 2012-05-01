<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<html>
  <head>
  	<title>OpenOffice Document Reader</title>
    <link rel="stylesheet" href="/static/dredit.css"></link>
    <script src="/static/jquery.min.js" type="text/javascript" charset="utf-8"></script>
    <script src="/static/dredit.js" type="text/javascript" charset="utf-8"></script>
  </head>
  <body>
  	Loading...
  </body>
  <script>
    var FILE_IDS = <%= request.getAttribute("ids") %>;

	// TODO: well, this doesn't make much sense, but let's just do it anyway. ;)
    for (i in FILE_IDS) {
    	get(FILE_IDS[i]);
    }
  </script>
</html>
