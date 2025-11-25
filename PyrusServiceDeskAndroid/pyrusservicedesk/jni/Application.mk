ifeq ($(OS),Windows_NT)
APP_SHORT_COMMANDS := true
endif

APP_ABI := all

include $(BUILD_SHARED_LIBRARY)

APP_SUPPORT_FLEXIBLE_PAGE_SIZES := true
