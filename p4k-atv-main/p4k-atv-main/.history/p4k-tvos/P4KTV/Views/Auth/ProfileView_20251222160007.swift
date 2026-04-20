import SwiftUI

struct ProfileView: View {
    @ObservedObject var userManager = UserManager.shared
    
    var body: some View {
        HStack(spacing: 50) {
            // Avatar / Icon
            Image(systemName: "person.circle.fill")
                .resizable()
                .frame(width: 200, height: 200)
                .foregroundColor(.gray)
            
            VStack(alignment: .leading, spacing: 20) {
                if let user = userManager.currentUser {
                    Text(user.name)
                        .font(.largeTitle)
                        .bold()
                    
                    Text(user.email)
                        .font(.title3)
                        .foregroundColor(.gray)
                    
                    HStack {
                        Text("Trạng thái gói:")
                        Text(user.status == "active" ? "Đã kích hoạt" : "Chưa kích hoạt")
                            .foregroundColor(user.status == "active" ? .green : .red)
                            .bold()
                    }
                    .font(.headline)
                    
                    NavigationLink(destination: SettingsView()) {
                        HStack {
                            Image(systemName: "gear")
                            Text("Cài Đặt")
                        }
                        .padding(.horizontal, 30)
                    }
                    .padding(.top, 10)
                    
                    Button(action: {
                        userManager.logout()
                    }) {
                        Text("Đăng Xuất")
                            .foregroundColor(.red)
                            .padding(.horizontal, 30)
                    }
                    .padding(.top, 20)
                    
                } else {
                    Text("Chưa đăng nhập")
                        .font(.largeTitle)
                    
                    NavigationLink(destination: LoginView()) {
                        Text("Đăng Nhập Ngay")
                            .padding(.horizontal, 30)
                    }
                }
            }
        }
        .padding(50)
    }
}
