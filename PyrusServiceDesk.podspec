Pod::Spec.new do |spec|

  spec.name         = 'PyrusServiceDesk'
  spec.version      = '1.3.0'
  spec.summary      = "Create a service with chat with support."
  spec.homepage     = 'https://pyrus.com'
  spec.license      = { :type => "MIT", :file => "LICENSE.txt" }
  spec.author       =  'Pyrus'
  spec.platform     = :ios, '9.0'
  spec.source       = { :git => 'https://github.com/simplygoodsoftware/pyrusservicedesk.git', :tag => "#{spec.version}" }
  spec.preserve_paths      = 'PyrusServiceDeskIOS/PyrusServiceDesk.framework'
  spec.vendored_frameworks = 'PyrusServiceDeskIOS/PyrusServiceDesk.framework'

end
