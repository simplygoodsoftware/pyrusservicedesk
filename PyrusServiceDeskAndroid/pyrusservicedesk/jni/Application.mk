ifeq ($(OS),Windows_NT)
APP_SHORT_COMMANDS := true
endif

APP_ABI := all

include $(BUILD_SHARED_LIBRARY)
