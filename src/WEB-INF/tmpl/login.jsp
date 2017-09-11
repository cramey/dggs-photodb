<%@
	page trimDirectiveWhitespaces="true"
%><!DOCTYPE html>
<html lang="en">
	<head>
		<title>Alaska Division of Geological &amp; Geophysical Surveys</title>
		<meta charset="utf-8">
		<meta http-equiv="x-ua-compatible" content="IE=edge" >
		<style>
			html { height: 100%; overflow-y: hidden; }
			body {
				height: 100%; overflow-y: auto; margin: 0px;
				background: #47638e;
				background: -webkit-gradient(linear, left top, left bottom, from(#47638e), to(#030c1b));
				background: -webkit-linear-gradient(top, #47638e, #0a306a, #030c1b);
				background: -moz-linear-gradient(top, #47638e, #0a306a, #030c1b);
				background: -o-linear-gradient(top, #47638e, #0a306a, #030c1b);
				filter: progid:DXImageTransform.Microsoft.gradient( startColorstr='#47638e', endColorstr='#030c1b', GradientType=0 );
				background: linear-gradient(top, #47638e, #0a306a, #030c1b);
				background-repeat: no-repeat;
				background-attachment: fixed;
			}
			.container {
				margin: 40px auto 0 auto;
				padding: 8px 16px 8px 16px;
				border: 1px solid #888;
				width: 300px;
				background-color: #fff;
				border-radius: 5px;
				text-align: center;
			}
			label {
				color: #444;
				display: block;
				font-size: 12px;
				text-align: left;
				margin: auto;
				width: 280px;
				font-family: Tahoma, Geneva, sans-serif;
			}
			input {
				display: block;
				margin: 0 auto 16px auto;
				width: 280px;
			}
			button {
				display: block;
				width: 280px;
				margin: 25px auto 0 auto;
			}
			h2 {
				margin: 0 8px 4px 8px;
				font-family: Tahoma, Geneva, sans-serif;
			}
		</style>
	</head>
	<body onload="document.getElementById('j_username').focus()">
		<div class="container">
			<h2>Photo Database</h2>

			<hr>

			<form method="post" action="j_security_check">
				<label for="j_username">Username</label>
				<input type="text" id="j_username" name="j_username">
				<label for="j_password">Password</label>
				<input type="password" name="j_password">
				<button>Log In</button>
			</form>
			<% if(request.getParameter("error") != null){ %>
			<hr>

			<span style="color: #f00">
				The username or password provided was incorrect.
				Please try again.
			</span>
			<% } %>
		</div>
	</body>
</html>
