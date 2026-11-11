# MyBoxMobs

Mob và boss tuỳ chỉnh cho server BoxPvP, dựng trên MythicMobs và ModelEngine. Admin tạo mob, phần thưởng, điểm spawn
và tế đàn triệu hồi boss hoàn toàn bằng GUI (`/mbm`), không cần viết file MythicMobs mới.

Paper 1.21.1, Java 21. Cần MythicMobs 5.13 (bản free). Không bắt buộc: ModelEngine (model 3D), DecentHolograms
(hologram), Vault (tiền), BoxCore (Gem, boost mob), BoxClans (thưởng theo bang), PlaceholderAPI, ItemsAdder.

## Mob

- Tạo từ mob MythicMobs có sẵn hoặc từ mob vanilla. Ghi đè chỉ số (máu, giáp, sát thương, tốc độ, tầm đuổi, kháng
  đẩy lùi), tên, trang bị 6 ô (không rơi), model ModelEngine (giữ model của MythicMobs hoặc thay, chỉnh kích thước).
- Dùng lại skill của MythicMobs: chọn skill trong GUI, đặt lúc kích hoạt (khi spawn, mỗi X giây, khi bị đánh, khi
  đánh trúng, khi chết), mục tiêu, thời gian hồi và tỉ lệ.
- Thời gian tồn tại, thanh máu trên tên và bossbar (bật riêng từng mob).
- Máu theo số đòn: mỗi đòn của người chơi trừ đúng 1 máu, lửa / rơi / mob khác không gây sát thương.
- Kỹ năng kẻ trộm: cướp % tiền hoặc Gem của người bị nó đánh, chạy trốn và dịch chuyển khi mất máu. Ai đánh trúng nó
  thì lấy lại được một phần, người hạ nó nhận phần còn lại; nó biến mất thì số đã cướp mất luôn.
- Mob được lưu cùng world như mob vanilla: chunk unload hay restart server vẫn còn, giữ nguyên bảng sát thương.

## Phần thưởng

Phần thưởng của mob thay hoàn toàn đồ rơi vanilla và MythicMobs. Mỗi mục là item (giữ nguyên NBT), tiền, Gem hoặc lệnh
console, có tỉ lệ và khoảng số lượng. Có 4 cách chia:

- **Người hạ**: người ra đòn cuối nhận.
- **Top sát thương**: hạng 1 đến N, mỗi hạng có tỉ lệ nhận riêng.
- **Top bang hội**: cộng sát thương theo clan; mỗi hạng clan nhận Danh Vọng, Minh Huy và quà cho thành viên đã đánh
  (cần BoxClans).
- **Quà bang hội**: cho boss của tế đàn bang hội, chia đều quà cho thành viên đã đánh.

Item vào thẳng túi đồ, túi đầy thì rơi dưới chân. Người offline lúc mob chết không nhận. Item, tiền và Gem được nhân
theo boost `mob` của BoxCore (quyền `boxcore.boost.mob.<n>`). Thông báo cả server khi mob xuất hiện, chết (kèm bảng xếp
hạng sát thương) hoặc biến mất.

## Điểm spawn

- Một hoặc nhiều mob có trọng số, số mob mỗi đợt, số mob sống tối đa, khoảng thời gian giữa các đợt.
- Kiểu đếm: theo chu kỳ, theo giờ trong ngày (`12:00 20:30`), hoặc hồi sinh từng con sau khi nó chết.
- Khu vực: chọn Pos1 / Pos2, mob xuất hiện ở chỗ đứng được ngẫu nhiên trong khu. Mob đi quá xa thì bị kéo về.
- Chỉ chạy khi có người ở gần (`NEARBY`) hoặc luôn chạy (`ALWAYS`), có thể giữ chunk luôn được load.
- Hologram DecentHolograms: đếm ngược, máu và sát thương của boss.
- Thời gian đếm được lưu lại, restart không bị đếm lại từ đầu.

## Tế đàn

Một block bất kỳ thành tế đàn: người chơi bấm vào với đúng item yêu cầu (bị lấy đi) để triệu hồi boss. Mỗi tế đàn một
boss mỗi lúc, có thời gian chờ. Block tế đàn tạm biến mất khi boss còn sống và trở lại nguyên dạng khi boss chết. Tế
đàn bang hội chỉ Minh Chủ triệu hồi được; boss thuộc về bang đó, chỉ thành viên bang đánh được nó. Item triệu hồi
không được trả lại.

## Dùng pack mob mua sẵn

Cài từng phần của pack vào đúng plugin (`.bbmodel` vào `plugins/ModelEngine/blueprints/`, file mob / skill vào
`plugins/MythicMobs/`, âm thanh và hình vào `plugins/ItemsAdder/contents/`), reload các plugin đó, rồi trong `/mbm`
chọn **Tạo từ MythicMobs** và chọn mob. Skill vẫn nằm trong file của MythicMobs.

## Lệnh

Tất cả cần `myboxmobs.admin` (mặc định op). `/mbm` không kèm gì mở GUI quản lý.

| Lệnh | Mô tả |
|---|---|
| `/mbm spawn <mob> [số lượng] [world x y z]` | Spawn mob (mặc định trước mặt 4 block) |
| `/mbm kill <mob> [world]`, `/mbm killall [world]` | Xoá mob đang sống |
| `/mbm list` | Mob đang sống, bấm để dịch chuyển tới |
| `/mbm spawner <create\|delete\|list\|tp\|toggle\|holo\|pos1\|pos2> [mã]` | Quản lý điểm spawn |
| `/mbm altar <create\|delete\|list> [mã] [mob]` | Quản lý tế đàn |
| `/mbm altar givekey <tế đàn> <người chơi> [số lượng]` | Đưa item triệu hồi (dùng được từ console, cho shop) |
| `/mbm reload` | Đọc lại file; mob đang sống dùng định nghĩa mới |

`myboxmobs.altar.use` (mặc định có): dùng tế đàn.

## Placeholder

| Placeholder | Giá trị |
|---|---|
| `%myboxmobs_kills_<mob>%` | Số lần người xem đã hạ mob này |
| `%myboxmobs_kills_total%` | Tổng số mob người xem đã hạ |
| `%myboxmobs_alive_<mob>%` | Số con đang sống |

## API

Module `myboxmobs-api`: `MyBoxMobsService` (gồm `ownerClanOf(entity)`), sự kiện `MbmMobSpawnEvent`, `MbmMobDeathEvent`,
`MbmMobDespawnEvent`.

## Build

```
./gradlew build
```

Ra file `myboxmobs-plugin/build/libs/MyBoxMobs-<version>.jar`. Repo chỉ chứa mã nguồn, không có jar nào ngoài
`gradle/wrapper/gradle-wrapper.jar` của Gradle. Trước khi build, đặt vào thư mục `libs/`:

- `MythicMobs-5.13.0.jar`: jar plugin MythicMobs (bản free)
- `ModelEngine-R4.1.1.jar`: jar plugin ModelEngine
- `DecentHolograms-2.10.1.jar`: jar plugin DecentHolograms
- `boxcore-api-1.3.0.jar`: build từ [BoxCore](https://github.com/FLD-TN/BoxCore) (`boxcore-api/build/libs/`)
- `boxclans-api-1.4.4.jar`: build từ [BoxClans](https://github.com/FLD-TN/BoxClans) (`boxclans-api/build/libs/`)
