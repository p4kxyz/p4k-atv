# Cloudflare Subtitle Worker API

## Base URL
- Dung domain Worker cua ban, vi du: `https://hehe.phim4k.lol`

## Muc tieu
- Lay danh sach phu de theo `file_id`
- Serve file SRT truc tiep cho player
- Neu chua co SRT tren R2 thi tu enqueue sang queue API de Colab xu ly

## Authentication
- Hai endpoint duoi day la public, khong can token:
- `GET /subtitles/{file_id}`
- `GET /serve/{file_id}/{filename}`

## CORS
- Da bat `Access-Control-Allow-Origin: *`
- Co the goi tu web player domain khac

## 1) Lay danh sach phu de
- Method: `GET`
- Path: `/subtitles/{file_id}`
- Vi du: `/subtitles/0EBA6500LO`

### Response 200
- `subtitles`: mang track de gan vao player
- `queued`:
- `null` neu da co phu de
- object neu chua co phu de va Worker vua enqueue sang hang cho

### Vi du response khi da co sub
```json
{
  "subtitles": [
    {
      "id": "vie_0.srt",
      "url": "https://hehe.phim4k.lol/serve/0EBA6500LO/vie_0.srt",
      "lang": "vie"
    },
    {
      "id": "eng_1.srt",
      "url": "https://hehe.phim4k.lol/serve/0EBA6500LO/eng_1.srt",
      "lang": "eng"
    }
  ],
  "queued": null
}
```

### Vi du response khi chua co sub (vua day vao queue)
```json
{
  "subtitles": [],
  "queued": {
    "queued": true,
    "reason": "queued"
  }
}
```

### Y nghia `queued.reason` thuong gap
- `queued`: vua them vao hang cho thanh cong
- `already_in_queue`: da co san trong hang cho
- `already_done`: da tung xu ly xong
- `already_error`: da tung loi
- `queue_api_not_configured`: thieu bien moi truong queue
- `enqueue_http_4xx` / `enqueue_http_5xx`: queue API tra ve loi HTTP

## 2) Lay file SRT
- Method: `GET`
- Path: `/serve/{file_id}/{filename}`
- Vi du: `/serve/0EBA6500LO/vie_0.srt`

### Response
- `200`: tra text SRT
- Header: `Content-Type: application/x-subrip; charset=utf-8`
- `404`: khong co file

## Flow tich hop player de xuat
1. Goi `GET /subtitles/{file_id}`
2. Neu `subtitles` co phan tu: add tung track vao player bang truong `url`
3. Neu `subtitles` rong:
- Hien thi trang thai "Phụ đề đang được tạo chờ 1-2 phút"
