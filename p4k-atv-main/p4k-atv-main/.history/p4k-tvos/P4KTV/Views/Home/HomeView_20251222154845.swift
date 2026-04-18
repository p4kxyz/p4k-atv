import SwiftUI

struct HomeView: View {
    @StateObject private var viewModel = HomeViewModel()
    
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 40) {
                if viewModel.isLoading {
                    ProgressView("Đang tải dữ liệu...")
                        .frame(maxWidth: .infinity, minHeight: 500)
                } else if let error = viewModel.errorMessage {
                    Text("Lỗi: \(error)")
                        .foregroundColor(.red)
                        .frame(maxWidth: .infinity, minHeight: 500)
                } else {
                    // Hero Banner
                    if let featured = viewModel.featuredVideo {
                        HeroBannerView(video: featured)
                    }
                    
                    // Sections
                    ForEach(viewModel.sections) { section in
                        VStack(alignment: .leading) {
                            Text(section.title)
                                .font(.headline)
                                .padding(.leading, 50)
                            
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 30) {
                                    ForEach(section.videos) { video in
                                        VideoCardView(video: video)
                                    }
                                }
                                .padding(.horizontal, 50)
                                .padding(.vertical, 20)
                            }
                        }
                    }
                }
            }
        }
        .edgesIgnoringSafeArea(.top)
        .onAppear {
            viewModel.loadData()
        }
    }
}
            }
        }
        .edgesIgnoringSafeArea(.top)
    }
}
