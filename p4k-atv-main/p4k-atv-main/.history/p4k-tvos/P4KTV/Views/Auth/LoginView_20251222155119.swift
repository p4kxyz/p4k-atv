import SwiftUI

struct LoginView: View {
    @State private var email = ""
    @State private var password = ""
    
    var body: some View {
        HStack {
            // Left side: Logo & Welcome
            VStack {
                Image(systemName: "play.tv.fill")
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(width: 200, height: 200)
                    .foregroundColor(.yellow)
                
                Text("Phim 4K")
                    .font(.system(size: 80, weight: .bold))
                
                Text("Đăng nhập để trải nghiệm")
                    .font(.title2)
                    .foregroundColor(.gray)
            }
            .frame(maxWidth: .infinity)
            
            // Right side: Form
            VStack(spacing: 30) {
                TextField("Email", text: $email)
                    .textContentType(.emailAddress)
                    .padding()
                    .background(Color.white.opacity(0.1))
                    .cornerRadius(10)
                
                SecureField("Mật khẩu", text: $password)
                    .textContentType(.password)
                    .padding()
                    .background(Color.white.opacity(0.1))
                    .cornerRadius(10)
                
                Button(action: {
                    // Login Action
                }) {
                    Text("Đăng Nhập")
                        .frame(maxWidth: .infinity)
                        .padding()
                }
                .padding(.top, 20)
                
                Text("Hoặc quét mã QR để đăng nhập nhanh")
                    .font(.caption)
                    .foregroundColor(.gray)
                    .padding(.top, 40)
                
                Image(systemName: "qrcode")
                    .resizable()
                    .frame(width: 150, height: 150)
            }
            .frame(width: 600)
            .padding(50)
            .background(Color.white.opacity(0.05))
            .cornerRadius(20)
        }
        .padding()
    }
}
