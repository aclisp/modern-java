@import com.mycompany.web.beans.IndexPage

@param IndexPage page

<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${page.title}</title>
</head>
<body>
    <h1>${page.title}</h1>
    <p>${page.content}</p>
</body>
</html>
