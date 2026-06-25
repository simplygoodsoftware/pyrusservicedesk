Pod::Spec.new do |spec|

spec.name         = 'PyrusServiceDesk'
<<<<<<< HEAD
spec.version      = '3.0.42'
=======
spec.version      = '3.0.61' 
>>>>>>> sb/sd/3.0.61
spec.summary      = "Create a service with chat with support."
spec.homepage     = 'https://pyrus.com'
spec.license      = { :type => "MIT", :file => "LICENSE.txt" }
spec.author       =  'Pyrus'
spec.platform     = :ios, '9.0'
spec.source       = { :git => 'https://github.com/simplygoodsoftware/pyrusservicedesk.git', :tag => "#{spec.version}" }
spec.preserve_paths      = 'PyrusServiceDeskIOS/Framework/PyrusServiceDesk.xcframework'
spec.vendored_frameworks = 'PyrusServiceDeskIOS/Framework/PyrusServiceDesk.xcframework'

end
