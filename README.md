# Libro.fm Audiobook Downloader

Small tool for checking your [libro.fm](https://libro.fm) library and downloading new books.

The tool is set to recheck the library every day and download new books. Books will be skipped if the `Author Name/Book Name` folder already exists.

## Extra Features

enable `RENAME_CHAPTERS` to rename files from `Track - #.mp3` to `### <Book Title> - <Chapter Title>` as provided by libro.fm

enable `WRITE_TITLE_TAG` to update each track's ID3 `title` field to `### <Chapter Title>` as provided by libro.fm.

bind a host port to `8080` to access the service's `/update` webhook. This allows you to manually force a refresh (ie: when you just purchased a book).

```
services:
  librofm-downloader:
    image: ghcr.io/burntcookie90/librofm-downloader:latest
    volumes:
      - /mnt/runtime/appdata/librofm-downloader:/data
      - /mnt/user/media/audiobooks:/media
    ports:
      # optional if you want to use the /update webhook
      - 8080:8080
    environment:
      - LIBRO_FM_USERNAME=<>
      - LIBRO_FM_PASSWORD=<>
      # extra optional: setting these enables them, dont add them if you dont want them.
      - DRY_RUN=true
      - VERBOSE=true
      - RENAME_CHAPTERS=true
      - WRITE_TITLE_TAG=true #this one requires RENAME_CHAPTERS to be true as well
      - SYNC_INTERVAL=h/d/w #choose one
      - DIRECTORY_TEMPLATE="<first author>/<if series-><series>/Book <series#> - <-if series><!if series->/<-if series><title>"
```

## Naming Templates

These are the template tags currently supported.

### Property Tags

These tags will be replaced with the audiobook's values.

| Tag             | Description            | Type   |
| --------------- | ---------------------- | ------ |
| \<first author> | The first author       | String |
| \<isbn>         | The book's ISBN        | Number |
| \<series>       | Name of the series     | String |
| \<series#>      | Number order in series | Number |
| \<title>        | Book's title           | String |

### Conditional Tags

Anything between the opening tag (\<tagname->) and closing tag (<-tagname>) will only appear in the name if the
condition evaluates to true.

| Tag                          | Description                      | Type        |
| ---------------------------- | -------------------------------- | ----------- |
| \<if series->...<-if series> | Only include if part of a series | Conditional |

You can invert the condition (instead of displaying the text when the condition is true, display the text when it is
false) by playing a '!' symbol before the opening tag name.
