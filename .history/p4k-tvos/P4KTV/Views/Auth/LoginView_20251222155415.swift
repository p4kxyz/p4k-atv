import SwiftUI

struct LoginView: View {
    @State private var email = ""
    @State private var password = ""
    @State private var errorMessage: String?
    @State private var isLoading = false
    @Environment(\.presentationMode) var presentationMode
    
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
                if let error = errorMessage {
                    Text(error)
                        .foregroundColor(.red)
                        .font(.headline)
                }
                
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
                
                Button(action: performLogin) {
                    if isLoading {
                        ProgressView()
                    } else {
                        Text("Đăng Nhập")
                            .frame(maxWidth: .infinity)
                            .padding()
                    }
                }
                .disabled(isLoading)
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
    
    func performLogin() {
        guard !email.isEmpty, !password.isEmpty else {
            errorMessage = "Vui lòng nhập đầy đủ thông tin"
            return
        }
        
        isLoading = true
        errorMessage = nil
        
        AuthService.shared.login(email: email, pass: password) { result in
            DispatchQueue.main.async {
                isLoading = false
                switch result {
                case .success(let user):
                    UserManager.shared.saveUser(user)
                    presentationMode.wrappedValue.dismiss()
                case .failure(let error):
                    errorMessage = error.localizedDescription
                }
            }
        }
    }
}
