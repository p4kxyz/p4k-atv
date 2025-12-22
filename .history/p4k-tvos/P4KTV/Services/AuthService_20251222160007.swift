import Foundation

class AuthService {
    static let shared = AuthService()
    
    func login(email: String, pass: String, completion: @escaping (Result<User, Error>) -> Void) {
        let params = [
            "email": email,
            "password": pass
        ]
        
        NetworkManager.shared.request(endpoint: "login", method: "POST", parameters: params) { (result: Result<[User], Error>) in
            switch result {
            case .success(let users):
                if let user = users.first {
                    completion(.success(user))
                } else {
                    completion(.failure(NSError(domain: "Login Failed", code: 401, userInfo: [NSLocalizedDescriptionKey: "Invalid credentials"])))
                }
            case .failure(let error):
                completion(.failure(error))
            }
        }
    }
}
