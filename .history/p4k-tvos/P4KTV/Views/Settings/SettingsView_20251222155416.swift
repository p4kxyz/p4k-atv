import SwiftUI

struct SettingsView: View {
    var body: some View {
        Form {
            Section(header: Text("Cài đặt chung")) {
                Toggle("Tự động phát video kế tiếp", isOn: .constant(true))
                Toggle("Hiển thị phụ đề mặc định", isOn: .constant(true))
            }
            
            Section(header: Text("Giao diện")) {
                Picker("Chủ đề", selection: .constant(0)) {
                    Text("Tối").tag(0)
                    Text("Sáng").tag(1)
                }
            }
            
            Section(header: Text("Thông tin ứng dụng")) {
                HStack {
                    Text("Phiên bản")
                    Spacer()
                    Text("1.0.0 (Build 1)")
                        .foregroundColor(.gray)
                }
                HStack {
                    Text("Liên hệ hỗ trợ")
                    Spacer()
                    Text("support@phim4k.lol")
                        .foregroundColor(.gray)
                }
            }
        }
        .navigationTitle("Cài Đặt")
    }
}
